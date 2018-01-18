/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.smugmug;

import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.dataportabilityproject.dataModels.photos.PhotosModelWrapper;
import org.dataportabilityproject.serviceProviders.smugmug.model.ImageUploadResponse;
import org.dataportabilityproject.serviceProviders.smugmug.model.SmugMugAlbum;
import org.dataportabilityproject.serviceProviders.smugmug.model.SmugMugAlbumImage;
import org.dataportabilityproject.serviceProviders.smugmug.model.SmugMugAlbumInfoResponse;
import org.dataportabilityproject.serviceProviders.smugmug.model.SmugMugResponse;
import org.dataportabilityproject.serviceProviders.smugmug.model.SmugMugUserResponse;
import org.dataportabilityproject.serviceProviders.smugmug.model.SmugmugAlbumResponse;
import org.dataportabilityproject.serviceProviders.smugmug.model.SmugmugAlbumsResponse;
import org.dataportabilityproject.shared.IdOnlyResource;
import org.dataportabilityproject.shared.StringPaginationToken;

final class SmugMugPhotoService implements
    Exporter<PhotosModelWrapper>,
    Importer<PhotosModelWrapper> {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final String BASE_URL = "https://api.smugmug.com";
  private static final String USER_URL = "/api/v2!authuser";

  private final OAuthConsumer authConsumer;
  private final HttpTransport httpTransport;
  private final JobDataCache jobDataCache;

  SmugMugPhotoService(OAuthConsumer authConsumer, JobDataCache jobDataCache) throws IOException {
    this.jobDataCache = jobDataCache;
    try {
      this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      this.authConsumer = authConsumer;
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Couldn't create smugmug api", e);
    }
  }

  private static InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }

  @Override
  public PhotosModelWrapper export(ExportInformation exportInformation) throws IOException {
    if (exportInformation.getResource().isPresent()) {
      return getImages(
          (IdOnlyResource) exportInformation.getResource().get(),
          exportInformation.getPaginationInformation());
    } else {
      return getAlbums(exportInformation.getPaginationInformation());
    }

  }

  @Override
  public void importItem(PhotosModelWrapper wrapper) throws IOException {
    String folder = null;
    if (!wrapper.getAlbums().isEmpty()) {
      SmugMugResponse<SmugMugUserResponse> userResponse = makeUserRequest(USER_URL);
      folder = userResponse.getResponse().getUser().getUris().get("Folder").getUri();
    }
    for (PhotoAlbum album : wrapper.getAlbums()) {
      createAlbum(folder, album);
    }

    for (PhotoModel photo : wrapper.getPhotos()) {
      uploadPhoto(photo);
    }
  }

  private void createAlbum(String folder, PhotoAlbum album) throws IOException {
    Map<String, String> json = new HashMap<>();
    String niceName = "Copy-" + album.getName().replace(' ', '-');
    json.put("UrlName", niceName);
    // Allow conflicting names to be changed
    json.put("AutoRename", "true");
    json.put("Name", "Copy of " + album.getName());
    // All imported content is private by default.
    json.put("Privacy", "Private");
    HttpContent content = new JsonHttpContent(new JacksonFactory(), json);
    SmugMugResponse<SmugmugAlbumResponse> response = postRequest(
        folder + "!albums",
        content,
        ImmutableMap.of(),
        new TypeReference<SmugMugResponse<SmugmugAlbumResponse>>() {
        });
    checkState(response.getResponse() != null, "Response is null");
    checkState(response.getResponse().getAlbum() != null, "Album is null");
    jobDataCache.store(album.getId(), response.getResponse().getAlbum().getAlbumKey());
  }

  private void uploadPhoto(PhotoModel photo) throws IOException {
    String newAlbumKey = jobDataCache.getData(photo.getAlbumId(), String.class);
    checkState(!Strings.isNullOrEmpty(newAlbumKey),
        "Cached album key for %s is null", photo.getAlbumId());

    InputStreamContent content = new InputStreamContent(null,
        getImageAsStream(photo.getFetchableUrl()));

    postRequest("http://upload.smugmug.com/",
        content,
        // Headers from: https://api.smugmug.com/api/v2/doc/reference/upload.html
        ImmutableMap.of(
            "X-Smug-AlbumUri", "/api/v2/album/" + newAlbumKey,
            "X-Smug-ResponseType", "json",
            "X-Smug-Version", "v2"),
        new TypeReference<ImageUploadResponse>() {
        });
  }

  private PhotosModelWrapper getImages(IdOnlyResource resource,
      Optional<PaginationInformation> paginationInformation) throws IOException {
    List<PhotoModel> photos = new ArrayList<>();

    String url;
    if (paginationInformation.isPresent()) {
      url = ((StringPaginationToken) paginationInformation.get()).getId();
    } else {
      String id = resource.getId();
      url = "/api/v2/album/" + id + "!images";
    }

    StringPaginationToken pageToken = null;

    SmugMugResponse<SmugMugAlbumInfoResponse> albumInfoResponse =
        makeAlbumInfoRequest(url);
    if (albumInfoResponse.getResponse().getImages() != null) {
      for (SmugMugAlbumImage image : albumInfoResponse.getResponse().getImages()) {
        String title = image.getTitle();
        if (Strings.isNullOrEmpty(title)) {
          title = image.getFileName();
        }

        try {
          photos.add(new PhotoModel(
              title,
              this.authConsumer.sign(image.getArchivedUri()),
              image.getCaption(),
              image.getFormat(),
              resource.getId()));
        } catch (OAuthException e) {
          throw new IOException("Couldn't sign: " + image.getArchivedUri(), e);
        }
      }

      if (albumInfoResponse.getResponse().getPageInfo().getNextPage() != null) {
        pageToken =
            new StringPaginationToken(albumInfoResponse.getResponse().getPageInfo().getNextPage());
      }
    }

    return new PhotosModelWrapper(null, photos, new ContinuationInformation(null, pageToken));
  }

  private PhotosModelWrapper getAlbums(
      Optional<PaginationInformation> paginationInformation) throws IOException {
    String albumUri;
    if (paginationInformation.isPresent()) {
      albumUri = ((StringPaginationToken) paginationInformation.get()).getId();
    } else {
      SmugMugResponse<SmugMugUserResponse> userResponse = makeUserRequest(USER_URL);
      albumUri = userResponse.getResponse().getUser().getUris().get("UserAlbums").getUri();
    }

    List<PhotoAlbum> albums = new ArrayList<>();
    List<Resource> resources = new ArrayList<>();

    SmugMugResponse<SmugmugAlbumsResponse> albumResponse = makeAlbumRequest(albumUri);
    for (SmugMugAlbum album : albumResponse.getResponse().getAlbums()) {
      albums.add(new PhotoAlbum(album.getAlbumKey(), album.getTitle(), album.getDescription()));
      resources.add(new IdOnlyResource(album.getAlbumKey()));
    }

    StringPaginationToken pageToken = null;
    if (albumResponse.getResponse().getPageInfo() != null
        && albumResponse.getResponse().getPageInfo().getNextPage() != null) {
      pageToken =
          new StringPaginationToken(albumResponse.getResponse().getPageInfo().getNextPage());
    }

    return new PhotosModelWrapper(albums, null, new ContinuationInformation(resources, pageToken));
  }

  private SmugMugResponse<SmugMugAlbumInfoResponse> makeAlbumInfoRequest(String url)
      throws IOException {
    return makeRequest(url, new TypeReference<SmugMugResponse<SmugMugAlbumInfoResponse>>() {
    });
  }

  private SmugMugResponse<SmugmugAlbumsResponse> makeAlbumRequest(String url) throws IOException {
    return makeRequest(url, new TypeReference<SmugMugResponse<SmugmugAlbumsResponse>>() {
    });
  }

  private SmugMugResponse<SmugMugUserResponse> makeUserRequest(String url) throws IOException {
    return makeRequest(url, new TypeReference<SmugMugResponse<SmugMugUserResponse>>() {
    });
  }

  private <T> SmugMugResponse<T> makeRequest(String url,
      TypeReference<SmugMugResponse<T>> typeReference) throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    String signedRequest;
    try {
      signedRequest = this.authConsumer.sign(BASE_URL + url + "?_accept=application%2Fjson");
    } catch (OAuthMessageSignerException
        | OAuthExpectationFailedException
        | OAuthCommunicationException e) {
      throw new IOException("Couldn't get albums", e);
    }
    HttpRequest getRequest = requestFactory.buildGetRequest(
        new GenericUrl(signedRequest));
    HttpResponse response = getRequest
        .execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException("Bad status code: " + statusCode + " error: "
          + response.getStatusMessage());
    }
    String result = CharStreams.toString(new InputStreamReader(
        response.getContent(), Charsets.UTF_8));
    return MAPPER.readValue(result, typeReference);
  }

  private <T> T postRequest(String url,
      HttpContent content,
      Map<String, String> headers,
      TypeReference<T> typeReference) throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

    String fullUrl = url;
    if (!fullUrl.contains("://")) {
      fullUrl = BASE_URL + url;
    }

    HttpRequest postRequest = requestFactory.buildPostRequest(
        new GenericUrl(fullUrl),
        content);
    HttpHeaders httpHeaders = new HttpHeaders()
        .setAccept("application/json")
        .setContentType("application/json");
    for (Entry<String, String> entry : headers.entrySet()) {
      httpHeaders.put(entry.getKey(), entry.getValue());
    }
    postRequest.setHeaders(httpHeaders);

    try {
      postRequest = (HttpRequest) this.authConsumer.sign(postRequest).unwrap();
    } catch (OAuthMessageSignerException
        | OAuthExpectationFailedException
        | OAuthCommunicationException e) {
      throw new IOException("Couldn't create post request", e);
    }

    HttpResponse response;
    try {
      response = postRequest.execute();
    } catch (HttpResponseException e) {
      throw new IOException("Problem making request: " + postRequest.getUrl(), e);
    }
    int statusCode = response.getStatusCode();
    if (statusCode < 200 || statusCode >= 300) {
      throw new IOException("Bad status code: " + statusCode + " error: "
          + response.getStatusMessage());
    }
    String result = CharStreams.toString(new InputStreamReader(
        response.getContent(), Charsets.UTF_8));

    return MAPPER.readValue(result, typeReference);
  }
}
