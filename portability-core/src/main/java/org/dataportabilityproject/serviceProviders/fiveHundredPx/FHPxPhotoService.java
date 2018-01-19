/*
 * Copyright 2018 Google Inc.
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
package org.dataportabilityproject.serviceProviders.fiveHundredPx;

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
import com.google.api.client.http.MultipartContent;
import com.google.api.client.http.MultipartContent.Part;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.photos.PhotoAlbum;
import org.dataportabilityproject.dataModels.photos.PhotoModel;
import org.dataportabilityproject.dataModels.photos.PhotosModelWrapper;
import org.dataportabilityproject.serviceProviders.fiveHundredPx.model.FHPxGallery;
import org.dataportabilityproject.serviceProviders.fiveHundredPx.model
    .FHPxPhotoUploadMetadataResponse;
import org.dataportabilityproject.serviceProviders.fiveHundredPx.model.FHPxResponse;

// TODO(olsona): address image sizing (1,2,3,...)
// TODO(olsona): what is the 500px equivalent of "/api/v2!authuser"?  Is there one?
// TODO(olsona): write custom mapper to address TRUE/FALSE coming up in JSON response
// TODO(olsona): address license types (for now, assume no problem)

final public class FHPxPhotoService implements Exporter<PhotosModelWrapper>,
    Importer<PhotosModelWrapper> {

  private static final ObjectMapper MAPPER = new ObjectMapper()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private static final String BASE_URL = "https://api.500px.com";

  private final OAuthConsumer authConsumer;
  private final HttpTransport httpTransport;
  private final JobDataCache jobDataCache;

  FHPxPhotoService(OAuthConsumer authConsumer, JobDataCache jobDataCache)
      throws IOException {
    this.jobDataCache = jobDataCache;
    try {
      this.httpTransport = GoogleNetHttpTransport.newTrustedTransport();
      this.authConsumer = authConsumer;
    } catch (GeneralSecurityException e) {
      throw new IllegalArgumentException("Couldn't create 500px API", e);
    }
  }

  @Override
  public PhotosModelWrapper export(ExportInformation continuationInformation) throws IOException {
    return null;
  }

  @Override
  public void importItem(PhotosModelWrapper wrapper) throws IOException {
    // TODO(olsona): What are we going to do about photo order in albums/galleries?

    Multimap<String, Integer> galleriesToPhotosMap = ArrayListMultimap.create();
    // Photos are uploaded first.
    for (PhotoModel photo : wrapper.getPhotos()) {
      int photoKey = uploadSinglePhoto(photo);
      galleriesToPhotosMap.put(photo.getAlbumId(), photoKey);
    }
    // Then upload albums, and tell albums which photos belong to them
    if (!wrapper.getAlbums().isEmpty()) {
      for (PhotoAlbum album : wrapper.getAlbums()) {
        int galleryId = createGallery(album);
        addPhotosToGallery(galleryId, galleriesToPhotosMap.get(album.getId()));
      }
    }
  }

  private int createGallery(PhotoAlbum album) throws IOException {
    String url = String.format("v1/users/%d/galleries", -1); // TODO(olsona): fix user value
    ImmutableMap<String, String> headersMap = ImmutableMap.of(); // TODO(olsona)

    Map<String, Object> json = new HashMap<>();
    json.put("name", album.getName());
    json.put("description", album.getDescription());
    json.put("privacy", 1); // default gallery to private
    json.put("kind", 0);
      // Generic gallery kind, see
      // https://github.com/500px/api-documentation/blob/master/basics/formats_and_terms.md#gallery-kinds
    HttpContent content = new JsonHttpContent(new JacksonFactory(), json);

    FHPxGallery response = postRequest(url, content, headersMap,
        new TypeReference<FHPxGallery>() {}).getResponse();
    return response.getId();
  }

  private void addPhotosToGallery(int galleryId, Collection<Integer> photoIds) throws IOException {
    String url = String.format("v1/users/%d/galleries/%d/items", -1, galleryId); // TODO(olsona): fix user value
    ImmutableMap<String, String> headersMap = ImmutableMap.of(); // TODO(olsona)

    Map<String, Map<String, Collection<Integer>>> json = new HashMap<>();
    json.put("add", ImmutableMap.of("photos", photoIds));
    HttpContent content = new JsonHttpContent(new JacksonFactory(), json);

    Map<String, Map<String, Object>> response = putRequest(url, content, headersMap,
        new TypeReference<Map<String, Map<String, Object>>>() {}).getResponse();
    // We can go through this list of responses and find out which additions were successful and which weren't
  }

  private int uploadSinglePhoto(PhotoModel photo) throws IOException {
    // Reference: https://github.com/500px/api-documentation/blob/master/basics/upload.md

    String metadataUrl = "v1/photos"; // TODO(olsona)
    ImmutableMap<String, String> metadataHeadersMap = ImmutableMap.of(); // TODO(olsona)
    // TODO(olsona): should this photo be private?

    Map<String, Object> json = new HashMap<>();
    json.put("name", photo.getTitle());
    json.put("description", photo.getDescription());
    json.put("privacy", 1); // Defaults to private
    HttpContent metadataContent = new JsonHttpContent(new JacksonFactory(), json);

    FHPxPhotoUploadMetadataResponse response = postRequest(metadataUrl, metadataContent,
        metadataHeadersMap,
        new TypeReference<FHPxPhotoUploadMetadataResponse>() {}).getResponse();

    ImmutableMap<String, String> contentHeadersMap = ImmutableMap.of(); // TODO(olsona)
    String contentUrl = response.getUrl();
    JsonHttpContent presignedPostContent = new JsonHttpContent(new JacksonFactory(), response.getPhoto());
    InputStreamContent imageContent = new InputStreamContent(null,
        getImageAsStream(photo.getFetchableUrl()));
    // TODO(olsona): verify this does what I think it does
    MultipartContent.Part presignedPart = new Part().setContent(presignedPostContent);
    MultipartContent.Part imagePart = new Part().setContent(imageContent);
    MultipartContent content = new MultipartContent().addPart(presignedPart).addPart(imagePart);

    postRequest(contentUrl, content, contentHeadersMap, new TypeReference<Object>() {}); // TODO(olsona)

    return response.getPhoto().getId();
  }

  // This could also be pulled out into a library.
  private <T> FHPxResponse<T> makeRequest(String url,
      TypeReference<FHPxResponse<T>> typeReference) throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
    String signedRequest;

    String fullUrl = url;
    if (!fullUrl.contains("://")) {
      fullUrl = BASE_URL + fullUrl;
    }

    try {
      signedRequest = this.authConsumer.sign(fullUrl + "?_accept=application%2Fjson");
    } catch (OAuthMessageSignerException
        | OAuthExpectationFailedException
        | OAuthCommunicationException e) {
      throw new IOException("Couldn't make request", e);
    }
    HttpRequest getRequest = requestFactory.buildGetRequest(new GenericUrl(signedRequest));
    HttpResponse response = getRequest.execute();
    int statusCode = response.getStatusCode();
    if (statusCode != 200) {
      throw new IOException(
          String.format("Bad status code: %d error: %s", statusCode, response.getStatusMessage()));
    }
    String result = CharStreams
        .toString(new InputStreamReader(response.getContent(), Charsets.UTF_8));
    return MAPPER.readValue(result, typeReference);
  }

  private <T> FHPxResponse<T> postRequest(String url, HttpContent content,
      Map<String, String> headers, TypeReference<T> typeReference) throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

    String fullUrl = url;
    if (!fullUrl.contains("://")) {
      fullUrl = BASE_URL + fullUrl;
    }

    HttpRequest postRequest = requestFactory.buildPostRequest(new GenericUrl(fullUrl), content);
    HttpHeaders httpHeaders = new HttpHeaders().setAccept("application/json")
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
      throw new IOException(
          String.format("Bad status code: %d error: %s", statusCode, response.getStatusMessage()));
    }
    String result = CharStreams.toString(new InputStreamReader(
        response.getContent(), Charsets.UTF_8));

    return MAPPER.readValue(result, typeReference);
  }

  private <T> FHPxResponse<T> putRequest(String url, HttpContent content,
      Map<String, String> headers, TypeReference<T> typeReference) throws IOException {
    HttpRequestFactory requestFactory = httpTransport.createRequestFactory();

    String fullUrl = url;
    if (!fullUrl.contains("://")) {
      fullUrl = BASE_URL + fullUrl;
    }

    HttpRequest putRequest = requestFactory.buildPutRequest(new GenericUrl(fullUrl), content);
    HttpHeaders httpHeaders = new HttpHeaders().setAccept("application/json")
        .setContentType("application/json");
    for (Entry<String, String> entry : headers.entrySet()) {
      httpHeaders.put(entry.getKey(), entry.getValue());
    }
    putRequest.setHeaders(httpHeaders);

    try {
      putRequest = (HttpRequest) this.authConsumer.sign(putRequest).unwrap();
    } catch (OAuthMessageSignerException
        | OAuthExpectationFailedException
        | OAuthCommunicationException e) {
      throw new IOException("Couldn't create put request", e);
    }

    HttpResponse response;
    try {
      response = putRequest.execute();
    } catch (HttpResponseException e) {
      throw new IOException("Problem making request: " + putRequest.getUrl(), e);
    }
    int statusCode = response.getStatusCode();
    if (statusCode < 200 || statusCode >= 300) {
      throw new IOException(
          String.format("Bad status code: %d error: %s", statusCode, response.getStatusMessage()));
    }
    String result = CharStreams.toString(new InputStreamReader(
        response.getContent(), Charsets.UTF_8));

    return MAPPER.readValue(result, typeReference);
  }

  // Could this be made available to all services?  Seems a broadly useful method.
  private static InputStream getImageAsStream(String urlStr) throws IOException {
    URL url = new URL(urlStr);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.connect();
    return conn.getInputStream();
  }
}
