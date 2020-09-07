/*
 * Copyright 2019 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.datatransfer.google.blogger;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.BlogList;
import com.google.api.services.blogger.model.Post;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.social.SocialActivityActor;
import org.datatransferproject.types.common.models.social.SocialActivityAttachment;
import org.datatransferproject.types.common.models.social.SocialActivityAttachmentType;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.common.models.social.SocialActivityType;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GoogleBloggerImporter
    implements Importer<TokensAndUrlAuthData, SocialActivityContainerResource> {
  private final GoogleCredentialFactory credentialFactory;
  private final ImageStreamProvider imageStreamProvider;
  // Don't access this directly, instead access via getOrCreateBloggerService.
  private Blogger blogger;
  // Don't access this directly, instead access via getOrCreateDriveService.
  // Used for image upload/hosting.
  private Drive driveInterface;

  public GoogleBloggerImporter(GoogleCredentialFactory credentialFactory) {
    this.credentialFactory = credentialFactory;

    this.imageStreamProvider = new ImageStreamProvider();
    // lazily initialized for the given request
    this.blogger = null;
    this.driveInterface = null;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      TokensAndUrlAuthData authData,
      SocialActivityContainerResource data)
      throws Exception {
    Blogger blogger = getOrCreateBloggerService(authData);

    BlogList blogList = blogger.blogs().listByUser("self").execute();

    // NB: we are just publishing everything to the first blog, which is a bit of a hack,
    // but there is no API to create a new blog.
    String blogId = blogList.getItems().get(0).getId();

    for (SocialActivityModel activity : data.getActivities()) {
      if (activity.getType() == SocialActivityType.NOTE
          || activity.getType() == SocialActivityType.POST) {
        try {
          insertActivity(idempotentExecutor, data.getActor(), activity, blogId, authData);
        } catch (IOException | RuntimeException e) {
          throw new IOException("Couldn't import: " + activity, e);
        }
      }
    }

    return new ImportResult(ResultType.OK);
  }

  private void insertActivity(
      IdempotentImportExecutor idempotentExecutor,
      SocialActivityActor actor,
      SocialActivityModel activity,
      String blogId,
      TokensAndUrlAuthData authData)
      throws Exception {
    String content = activity.getContent() == null ? "" : activity.getContent();

    Collection<SocialActivityAttachment> linkAttachments =
        activity.getAttachments().stream()
            .filter(attachment -> attachment.getType() == SocialActivityAttachmentType.LINK)
            .collect(Collectors.toList());
    Collection<SocialActivityAttachment> imageAttachments =
        activity.getAttachments().stream()
            .filter(attachment -> attachment.getType() == SocialActivityAttachmentType.IMAGE)
            .collect(Collectors.toList());

    // Just put link attachments at the bottom of the post, as we
    // don't know how they were laid out in the originating service.
    for (SocialActivityAttachment attachment : linkAttachments) {
      content =
          "<a href=\""
              + attachment.getUrl()
              + "\">"
              + attachment.getName()
              + "</a>\n</hr>\n"
              + content;
    }

    if (!imageAttachments.isEmpty()) {
      // Store any attached images in Drive in a new folder.
      Drive driveInterface = getOrCreateDriveService(authData);
      String folderId =
          idempotentExecutor.executeOrThrowException(
              "MainAlbum", "Photo Album", () -> createAlbumFolder(driveInterface));
      for (SocialActivityAttachment image : imageAttachments) {
        try {
          String newImgSrc =
              idempotentExecutor.executeAndSwallowIOExceptions(
                  image.toString(), "Image", () -> uploadImage(image, driveInterface, folderId));
          content += "\n<hr/><img src=\"" + newImgSrc + "\">";
        } catch (RuntimeException e) {
          throw new IOException("Couldn't import: " + imageAttachments, e);
        }
      }
    }

    String title = "";

    if (activity.getTitle() != null && !Strings.isNullOrEmpty(activity.getTitle())) {
      title = activity.getTitle();
    }

    Post post = new Post().setTitle("Imported post: " + title).setContent(content);
    if (actor != null) {
      Post.Author author = new Post.Author();
      if (!Strings.isNullOrEmpty(actor.getName())) {
        author.setDisplayName(actor.getName());
      }
      if (!Strings.isNullOrEmpty(actor.getUrl())) {
        author.setUrl(actor.getUrl());
      }
      post.setAuthor(author);
    }
    if (activity.getPublished() != null) {
      post.setPublished(new DateTime(activity.getPublished().toEpochMilli()));
    }

    idempotentExecutor.executeAndSwallowIOExceptions(
        title,
        title,
        () ->
            getOrCreateBloggerService(authData)
                .posts()
                .insert(blogId, post)
                // Don't publish directly, ensure that the user explicitly reviews
                // and approves content first.
                .setIsDraft(true)
                .execute()
                .getId());
  }

  private String createAlbumFolder(Drive driveInterface) throws IOException {
    File fileMetadata = new File();
    LocalDate localDate = LocalDate.now();
    fileMetadata.setName("(Public)Imported Images on: " + localDate.toString());
    fileMetadata.setMimeType("application/vnd.google-apps.folder");
    File folder = driveInterface.files().create(fileMetadata).setFields("id").execute();
    driveInterface
        .permissions()
        .create(
            folder.getId(),
            // Set link sharing on, see:
            // https://developers.google.com/drive/api/v3/reference/permissions/create
            new Permission().setRole("reader").setType("anyone").setAllowFileDiscovery(false))
        .execute();
    return folder.getId();
  }

  private String uploadImage(
      SocialActivityAttachment imageObject, Drive driveService, String parentFolderId)
      throws IOException {
    String url;
    url = imageObject.getUrl().toString();

    String description =
        imageObject.getName() != null ? imageObject.getName() : ("Imported photo from: " + url);

    HttpURLConnection conn = imageStreamProvider.getConnection(url);
    InputStream inputStream = conn.getInputStream();
    File driveFile = new File().setName(description).setParents(ImmutableList.of(parentFolderId));
    InputStreamContent content = new InputStreamContent(null, inputStream);
    File newFile = driveService.files().create(driveFile, content).setFields("id").execute();

    return "https://drive.google.com/thumbnail?id=" + newFile.getId();
  }

  private Blogger getOrCreateBloggerService(TokensAndUrlAuthData authData) {
    return blogger == null ? makeBloggerService(authData) : blogger;
  }

  private synchronized Blogger makeBloggerService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Blogger.Builder(
            credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }

  private synchronized Drive getOrCreateDriveService(TokensAndUrlAuthData authData) {
    return driveInterface == null ? (driveInterface = makeDriveService(authData)) : driveInterface;
  }

  private synchronized Drive makeDriveService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Drive.Builder(
            credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }
}
