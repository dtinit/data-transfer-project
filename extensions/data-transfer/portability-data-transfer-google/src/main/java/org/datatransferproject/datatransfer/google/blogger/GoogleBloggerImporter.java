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
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.BlogList;
import com.google.api.services.blogger.model.Post;
import com.google.common.base.Strings;
import com.ibm.common.activitystreams.ASObject;
import com.ibm.common.activitystreams.Activity;
import com.ibm.common.activitystreams.LinkValue;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.UUID;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.datatransfer.google.photos.GooglePhotosInterface;
import org.datatransferproject.datatransfer.google.photos.model.BatchMediaItemResponse;
import org.datatransferproject.datatransfer.google.photos.model.GoogleAlbum;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItem;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItemResult;
import org.datatransferproject.datatransfer.google.photos.model.NewMediaItemUpload;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.provider.ImportResult;
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
  private final JsonFactory jsonFactory;
  private final ImageStreamProvider imageStreamProvider;
  private Blogger blogger;
  private GooglePhotosInterface photosInterface;

  public GoogleBloggerImporter(
      GoogleCredentialFactory credentialFactory,
      JobStore jobStore,
      JsonFactory jsonFactory) {
    this.credentialFactory = credentialFactory;
    this.jobStore = jobStore;
    this.jsonFactory = jsonFactory;
    this.imageStreamProvider = new ImageStreamProvider();
    // lazily initialized for the given request
    this.blogger = null;
    this.photosInterface = null;
  }

  @Override
  public ImportResult importItem(UUID jobId,
      TokensAndUrlAuthData authData,
      SocialActivityContainerResource data) throws Exception {
    Blogger blogger = getOrCreateBloggerService(authData);

    BlogList blogList = blogger.blogs().listByUser("self").execute();

    // TODO: we are just publishing everything to the first blog, which is a bit of a hack
    String blogId = blogList.getItems().get(0).getId();

    for (Activity activity : data.getActivities()) {
      for (LinkValue object : activity.object()) {
        checkState(object instanceof ASObject, "%s isn't of expected type", object);
        ASObject asObject = (ASObject) object;
        if (asObject.objectTypeString().equals("note")
            || asObject.objectTypeString().equals("post")) {
          try {
            insertActivity(jobId, activity, asObject, blogId, authData);
          } catch (IOException | RuntimeException e) {
            throw new IOException("Couldn't import: " + activity, e);
          }
        }
      }
    }

    return null;
  }

  private void insertActivity(
      UUID jobId,
      Activity activity,
      ASObject asObject,
      String blogId,
      TokensAndUrlAuthData authData)
      throws IOException {
    System.out.println("Importing: " + activity);

    String content = asObject.content() == null ? "" : asObject.contentString();
    System.out.println("asObject.content(): " + asObject.content());
    if ("null".equals(content)) {
      System.out.println("Content was null");
      content = "";
    }
    System.out.println("Original Content: " + content);

    for (LinkValue attachmentLinkValue : asObject.attachments()) {
      ASObject attachment = (ASObject) attachmentLinkValue;
      content = "<a href=\"" + attachment.firstUrl().toString() +"\">"
          + attachment.displayNameString() + "</a>\n</hr>\n"
          + content;
      System.out.println("New Content: " + content);
    }

    if (asObject.firstImage() != null) {
      GooglePhotosInterface photosInterface = getOrCreatePhotosInterface(authData);
      for (LinkValue image : asObject.image()) {
        try {
          String newImgSrc = uploadImage(jobId, (ASObject) image, photosInterface);
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

  private String uploadImage(
      UUID jobId,
      ASObject imageObject,
      GooglePhotosInterface photosInterface)
      throws IOException {
    String url;
    String description = null;
    System.out.println("image object: " + imageObject.objectTypeString() + " " + imageObject.getClass().getCanonicalName());
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

    String uploadToken = photosInterface.uploadPhotoContent(inputStream);

    NewMediaItem newMediaItem = new NewMediaItem(description, uploadToken);

    BloggerAlbumStore albumId = jobStore.findData(jobId, ALBUM_ID_KEY, BloggerAlbumStore.class);
    if (albumId == null || Strings.isNullOrEmpty(albumId.getAlbumId())) {
      GoogleAlbum newAlbum = photosInterface.createAlbum(
          new GoogleAlbum().setTitle("Imported Photos For Blogger"));
      albumId = new BloggerAlbumStore(newAlbum.getId());
      jobStore.create(jobId, ALBUM_ID_KEY, albumId);
    }

    NewMediaItemUpload uploadItem =
        new NewMediaItemUpload(albumId.getAlbumId(), Collections.singletonList(newMediaItem));

    BatchMediaItemResponse results = photosInterface.createPhoto(uploadItem);
    System.out.println("Upload result: " + results);
    if (results.getResults().length != 1) {
      throw new IllegalStateException("Didn't get exactly one response to creating a photo: "
          + results);
    }
    NewMediaItemResult result = results.getResults()[0];
    checkState(result.getStatus().getCode() == 0, "Bad status code for upload %s", results);
    String uploadedPhotoUrl = result.getMediaItem().getProductUrl();
    checkState(
        !Strings.isNullOrEmpty(uploadedPhotoUrl),
        "No url found for uploaded photo %s",
        results);

    return uploadedPhotoUrl;
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

  private synchronized GooglePhotosInterface getOrCreatePhotosInterface(
      TokensAndUrlAuthData authData) {
    return photosInterface == null ? makePhotosInterface(authData) : photosInterface;
  }

  private synchronized GooglePhotosInterface makePhotosInterface(TokensAndUrlAuthData authData) {
    Credential credential = credentialFactory.createCredential(authData);
    GooglePhotosInterface photosInterface = new GooglePhotosInterface(credential, jsonFactory);
    return photosInterface;
  }

  @JsonTypeName("org.dataportability.google:BloggerData")
  private static class BloggerAlbumStore extends DataModel {
    @JsonProperty("albumId")
    private final String albumId;

    @JsonCreator
    public BloggerAlbumStore(@JsonProperty("albumId") String albumId) {
      this.albumId = albumId;
    }

    String getAlbumId() {
      return albumId;
    }
  }
}
