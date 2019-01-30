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
import com.google.api.client.util.DateTime;
import com.google.api.services.blogger.Blogger;
import com.google.api.services.blogger.model.BlogList;
import com.google.api.services.blogger.model.Post;
import com.google.api.services.blogger.model.Post.Images;
import com.google.common.base.Strings;
import com.ibm.common.activitystreams.ASObject;
import com.ibm.common.activitystreams.Activity;
import com.ibm.common.activitystreams.LinkValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.datatransfer.google.common.GoogleStaticObjects;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.UUID;

import static com.google.common.base.Preconditions.checkState;

public class GoogleBloggerImporter implements Importer<TokensAndUrlAuthData, SocialActivityContainerResource> {
  private final GoogleCredentialFactory credentialFactory;
  private Blogger blogger;

  public GoogleBloggerImporter(GoogleCredentialFactory credentialFactory) {
    this.credentialFactory = credentialFactory;
    this.blogger = null; // lazily initialized for the given request
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
            insertActivity(activity, asObject, blogId, authData);
          } catch (IOException | RuntimeException e) {
            throw new IOException("Couldn't import: " + activity, e);
          }
        }
      }
    }

    return null;
  }

  private void insertActivity(
      Activity activity,
      ASObject asObject,
      String blogId,
      TokensAndUrlAuthData authData)
      throws IOException {
    System.out.println("Importing: " + activity);

    String content = asObject.content() == null ? "" : asObject.contentString();
    if ("null".equals(content)) {
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

    for (LinkValue image : asObject.image()) {
      // TODO: upload images to photos and then insert link here..., kind of gooney
    }

    getOrCreateBloggerService(authData).posts()
        .insert(blogId, post)
        // Don't publish directly, ensure that the user explicitly reviews
        // and approves content first.
        .setIsDraft(true)
        .execute();
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
}
