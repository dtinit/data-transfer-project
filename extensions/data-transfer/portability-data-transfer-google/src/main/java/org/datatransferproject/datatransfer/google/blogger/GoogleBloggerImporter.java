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

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
import com.ibm.common.activitystreams.ASObject;
import com.ibm.common.activitystreams.Activity;
import com.ibm.common.activitystreams.LinkValue;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.ImportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.ImageStreamProvider;
import org.datatransferproject.types.common.models.DataModel;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class GoogleBloggerImporter
    implements Importer<TokensAndUrlAuthData, SocialActivityContainerResource> {
  private static final String ALBUM_ID_KEY = "BloggerAlbumId";
  private final GoogleCredentialFactory credentialFactory;
  private final JobStore jobStore;
  private final ImageStreamProvider imageStreamProvider;
  // Don't access this directly, instead access via getOrCreateBloggerService.
  private Blogger blogger;
  // Don't access this directly, instead access via getOrCreateDriveService.
  // Used for image upload/hosting.
  private Drive driveInterface;

  public GoogleBloggerImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;

    this.imageStreamProvider = new ImageStreamProvider();
    // lazily initialized for the given request
    this.blogger = null;
    this.driveInterface = null;
  }

  @Override
  public ImportResult importItem(UUID jobId,
      TokensAndUrlAuthData authData,
      SocialActivityContainerResource data) throws Exception {
    Blogger blogger = getOrCreateBloggerService(authData);

    BlogList blogList = blogger.blogs().listByUser("self").execute();

    // NB: we are just publishing everything to the first blog, which is a bit of a hack,
    // but there is no API to create a new blog.
    String blogId = blogList.getItems().get(0).getId();

    for (Activity activity : data.getActivities()) {
      for (LinkValue object : activity.object()) {
        checkState(object instanceof ASObject, "%s isn't of expected type", object);
        ASObject asObject = (ASObject) object;
        if (asObject.objectTypeString().equalsIgnoreCase("note")
            || asObject.objectTypeString().equalsIgnoreCase("post")) {
          try {
            insertActivity(jobId, activity, asObject, blogId, authData);
          } catch (IOException | RuntimeException e) {
            throw new IOException("Couldn't import: " + activity, e);
          }
        }
      }
    }

    return new ImportResult(ResultType.OK);
  }

  private void insertActivity(
      UUID jobId,
      Activity activity,
      ASObject asObject,
      String blogId,
      TokensAndUrlAuthData authData)
      throws IOException {
    String content = asObject.content() == null ? "" : asObject.contentString();

    if (content == null) {
      content = "";
    }

    // Just put link attachments at the bottom of the post, as we
    // don't know how they were laid out in the originating service.
    for (LinkValue attachmentLinkValue : asObject.attachments()) {
      ASObject attachment = (ASObject) attachmentLinkValue;
      content = "<a href=\"" + attachment.firstUrl().toString() +"\">"
          + attachment.displayNameString() + "</a>\n</hr>\n"
          + content;
    }

    if (asObject.firstImage() != null) {
      // Store any attached images in Drive in a new folder.
      Drive driveInterface = getOrCreateDriveService(authData);
      String folderId = createAlbumFolder(driveInterface, jobId);
      for (LinkValue image : asObject.image()) {
        try {
          String newImgSrc = uploadImage((ASObject) image, driveInterface, folderId);
          content += "\n<hr/><img src=\"" + newImgSrc + "\">";
        } catch (IOException | RuntimeException e) {
          throw new IOException("Couldn't import: " + asObject.image(), e);
        }
      }
    }

    String title = "";

    String provider = null;

    if (asObject.provider() != null) {
      provider = asObject.firstProvider().toString();
    }

    if (asObject.title() != null && !Strings.isNullOrEmpty(asObject.titleString())) {
      title = asObject.titleString();
    } if (asObject.displayName() != null && !Strings.isNullOrEmpty(asObject.displayNameString())) {
      title = asObject.displayNameString();
    }

    Post post = new Post()
        .setTitle("Imported " + provider+ " post: " + title)
        .setContent(content);
    if (activity.firstActor() != null) {
      Post.Author author = new Post.Author();
      ASObject actorObject = (ASObject) activity.firstActor();
      if (!Strings.isNullOrEmpty(actorObject.displayNameString())) {
        author.setDisplayName(actorObject.displayNameString());
      }
      if (actorObject.firstUrl() != null
          && !Strings.isNullOrEmpty(actorObject.firstUrl().toString())) {
        author.setUrl(actorObject.firstUrl().toString());
      }
      post.setAuthor(author);
    }
    if (asObject.published() != null) {
      post.setPublished(new DateTime(asObject.published().getMillis()));
    }

    getOrCreateBloggerService(authData).posts()
        .insert(blogId, post)
        // Don't publish directly, ensure that the user explicitly reviews
        // and approves content first.
        .setIsDraft(true)
        .execute();
  }

  private String createAlbumFolder(Drive driveInterface, UUID jobId) throws IOException {
    BloggerAlbumStore albumData = jobStore.findData(jobId, ALBUM_ID_KEY, BloggerAlbumStore.class);
    if (albumData != null && !Strings.isNullOrEmpty(albumData.getDriveFolderId())) {
      return albumData.getDriveFolderId();
    }
    File fileMetadata = new File();
    LocalDate localDate = LocalDate.now();
    fileMetadata.setName("(Public)Imported Images on: " + localDate.toString());
    fileMetadata.setMimeType("application/vnd.google-apps.folder");
    File folder = driveInterface.files().create(fileMetadata)
        .setFields("id")
        .execute();
    driveInterface.permissions()
        .create(
            folder.getId(),
            // Set link sharing on, see:
            // https://developers.google.com/drive/api/v3/reference/permissions/create
            new Permission()
                .setRole("reader")
                .setType("anyone")
                .setAllowFileDiscovery(false))
        .execute();
    jobStore.create(jobId, ALBUM_ID_KEY, new BloggerAlbumStore(folder.getId()));
    return folder.getId();
  }

  private String uploadImage(
      ASObject imageObject,
      Drive driveService,
      String parentFolderId)
      throws IOException {
    String url;
    String description = null;
    // The image property can either be an object, or just a URL, handle both cases.
    if ("Image".equalsIgnoreCase(imageObject.objectTypeString())) {
      url = imageObject.firstUrl().toString();
      if (imageObject.displayName() != null) {
        description = imageObject.displayNameString();
      }
    } else {
      url = imageObject.toString();
    }
    if (description == null) {
      description = "Imported photo from: " + url;
    }
    InputStream inputStream = imageStreamProvider.get(url);
    File driveFile = new File()
        .setName(description)
        .setParents(ImmutableList.of(parentFolderId));
    InputStreamContent content = new InputStreamContent(null, inputStream);
    File newFile = driveService.files().create(driveFile, content)
        .setFields("id")
        .execute();

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

  private synchronized Drive getOrCreateDriveService(
      TokensAndUrlAuthData authData) {
    return driveInterface == null ? (driveInterface = makeDriveService(authData)) : driveInterface;
  }

  private synchronized Drive makeDriveService(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    return new Drive.Builder(
        credentialFactory.getHttpTransport(), credentialFactory.getJsonFactory(), credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }

  @JsonTypeName("org.dataportability.google:BloggerAlbumData")
  private static class BloggerAlbumStore extends DataModel {
    @JsonProperty("driveFolderId")
    private final String driveFolderId;

    @JsonCreator
    BloggerAlbumStore(@JsonProperty("driveFolderId") String driveFolderId) {
      this.driveFolderId = driveFolderId;
    }

    String getDriveFolderId() {
      return driveFolderId;
    }
  }
}