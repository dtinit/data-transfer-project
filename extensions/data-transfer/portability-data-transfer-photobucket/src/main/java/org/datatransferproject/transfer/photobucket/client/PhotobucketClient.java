/*
 * Copyright 2021 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.photobucket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.Credential;
import okhttp3.*;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.transfer.photobucket.client.helper.InputStreamRequestBody;
import org.datatransferproject.transfer.photobucket.data.PhotobucketAlbum;
import org.datatransferproject.transfer.photobucket.data.response.gql.PhotobucketGQLResponse;
import org.datatransferproject.transfer.photobucket.data.ProcessingResult;
import org.datatransferproject.transfer.photobucket.data.response.rest.UploadMediaResponse;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Function;

import static org.datatransferproject.transfer.photobucket.data.PhotobucketConstants.*;

public class PhotobucketClient {
  private final String bearer;
  private final OkHttpClient httpClient;
  private final TemporaryPerJobDataStore jobStore;
  private final UUID jobId;
  private String pbRootAlbumId;
  private final ObjectMapper objectMapper;

  public PhotobucketClient(
      UUID jobId,
      Credential credential,
      OkHttpClient httpClient,
      TemporaryPerJobDataStore jobStore,
      ObjectMapper objectMapper) {
    this.jobId = jobId;
    this.bearer = "Bearer " + credential.getAccessToken();
    this.httpClient = httpClient;
    this.jobStore = jobStore;
    this.objectMapper = objectMapper;
  }

  public void createTopLevelAlbum(String name) throws Exception {
    // in case if albumId was not found while migrating photos,
    // we will migrate them into this top level album to avoid data loss
    PhotoAlbum photoAlbum = new PhotoAlbum(jobId.toString(), name, "");
    createAlbum(photoAlbum, "");
  }

  public void createAlbum(PhotoAlbum photoAlbum, String namePrefix) throws Exception {
    try {
      // check if album was not created before
      getPBAlbumId(photoAlbum.getId());
    } catch (Exception exception) {
      // if album was not created
      // generate gql query for getting pb album id via rest call
      RequestBody requestBody = createAlbumGQLViaRestMutation(photoAlbum, namePrefix);
      // get pbAlbumId and save it into job store
      Function<Response, ProcessingResult> bodyTransformF =
          response -> {
            try {
              // get photobucket albumId from response
              String pbAlbumId = parseGQLResponse(response).getCreatedAlbumId();
              // add photobucket albumId to the internal store, to match photos with proper albums
              jobStore.create(jobId, photoAlbum.getId(), new PhotobucketAlbum(pbAlbumId));
              return new ProcessingResult(pbAlbumId);
            } catch (IOException e) {
              return new ProcessingResult(e);
            }
          };

      // throw exception only if failed for top level album
      Function<Void, Boolean> isTopLevelAlbumF = v -> photoAlbum.getId().equals(jobId.toString());

      // fallback result in case if query execution/result parsing failed. In case if it's top level
      // album, we need to provide null to throw an exception. If it's usual album we can proceed.
      ProcessingResult fallbackResult =
          isTopLevelAlbumF.apply(null) ? null : new ProcessingResult("");

      System.out.println("Creating album " + photoAlbum.getName());
      ProcessingResult result =
          performGQLRequest(requestBody, bodyTransformF, isTopLevelAlbumF, fallbackResult);
      result.extractOrThrow();
    }
  }

  public void uploadPhoto(PhotoModel photoModel) throws IOException {
    RequestBody uploadRequestBody;
    String url;
    // get pbAlbumId based on provided albumId
    String pbAlbumId = getPBAlbumId(photoModel.getAlbumId());
    if (photoModel.isInTempStore()) {
      // stream file
      BufferedInputStream inputStream =
          new BufferedInputStream(
              jobStore.getStream(jobId, photoModel.getFetchableUrl()).getStream());
      uploadRequestBody =
          new InputStreamRequestBody(MediaType.parse(photoModel.getMediaType()), inputStream);
      url = String.format("%s?albumId=%s&name=%s", UPLOAD_URL, pbAlbumId, photoModel.getTitle());
    } else if (photoModel.getFetchableUrl() != null) {
      // upload media file via url
      // add query parameters
      url =
          String.format(
              "%s?url=%s&albumId=%s", UPLOAD_BY_URL_URL, photoModel.getFetchableUrl(), pbAlbumId);
      uploadRequestBody = new FormBody.Builder().build();
    } else {
      throw new IllegalStateException(
          "Unable to get input stream for image " + photoModel.getTitle());
    }

    if (isUserOveStorage(uploadRequestBody.contentLength())) {
      throw new IllegalStateException("User reached his storage limits");
    }

    Request.Builder uploadRequestBuilder = new Request.Builder().url(url);
    // add authorization headers
    uploadRequestBuilder
        .header(AUTHORIZATION_HEADER, bearer)
        .header(CORRELATION_ID_HEADER, jobId.toString());
    // POST with empty body request
    uploadRequestBuilder.post(uploadRequestBody);
    Response uploadImageResponse = httpClient.newCall(uploadRequestBuilder.build()).execute();
    if (uploadImageResponse.code() == 201) {
      // note: if 201 code was provided, but response value is empty, do not fail upload, just skip
      // title/description update
      if (uploadImageResponse.body() != null
          && photoModel.getDescription() != null
          && !photoModel.getDescription().isEmpty()) {
        String description = photoModel.getDescription().replace("\"", "").replace("\n", " ");
        // get imageId from provided response
        String imageId =
            objectMapper.readValue(uploadImageResponse.body().string(), UploadMediaResponse.class)
                .id;
        // update metadata gql query
        RequestBody requestBody =
            RequestBody.create(
                MediaType.parse("application/json"),
                String.format(
                    "{\"query\": \"mutation updateImageDTP($imageId: String!, $title: String!, $description: String){ updateImage(imageId: $imageId, title: $title, description: $description)}\", \"variables\": {\"imageId\": \"%s\", \"title\": \"%s\", \"description\": \"%s\"}}",
                    imageId, photoModel.getTitle(), description));

        // do not verify update metadata response
        Function<Response, ProcessingResult> bodyTransformF =
            response -> new ProcessingResult(imageId);

        // newer fail in case of error
        Function<Void, Boolean> conditionalExceptionF = v -> true;

        try {
          // add metadata via gql
          performGQLRequest(requestBody, bodyTransformF, conditionalExceptionF, null);
        } catch (Exception ignored) {
          System.out.println(
              "Photo update wasn't successful: " + photoModel.getTitle() + " " + description);
        }
      }
    } else {
      // throw error in case upload was not successful
      throw new IOException(
          String.format(
              "Wrong status code=[%s], message=[%s] provided by REST for jobId=[%s]",
              uploadImageResponse.code(), uploadImageResponse.message(), jobId));
    }
  }

  private ProcessingResult performGQLRequest(
      RequestBody requestBody,
      Function<Response, ProcessingResult> responseTransformF,
      Function<Void, Boolean> conditionalExceptionF,
      ProcessingResult fallbackResult)
      throws IOException {

    // create builder for graphQL request
    Request.Builder gqlRequestBuilder = new Request.Builder().url(GQL_URL);
    // add authorization headers
    gqlRequestBuilder
        .header(AUTHORIZATION_HEADER, bearer)
        .header(CORRELATION_ID_HEADER, jobId.toString())
        .header(REFERER_HEADER, REFERER_HEADER_VALUE)
        .header(ORIGIN_HEADER, ORIGIN_HEADER_VALUE);
    gqlRequestBuilder.post(requestBody);
    // post request
    Response response = httpClient.newCall(gqlRequestBuilder.build()).execute();
    // gql server always provides 200 response code. If not, interrupt job
    if (response.code() == 200) {
      try {
        // apply transformation function to the response
        return responseTransformF.apply(response);
      } catch (Exception e) {
        // throw error only for given rules
        if (conditionalExceptionF.apply(null)) {
          throw e;
        } else {
          return fallbackResult;
        }
      }
    } else {
      throw new IOException(
          String.format(
              "Wrong status code=[%s] provided by GQL server for jobId=[%s]",
              response.code(), jobId));
    }
  }

  /**
   * Create album either under pbRoot album (in case if we create top album) or under top album
   * TODO: add description while album creation, not supported for now within the same call
   */
  private RequestBody createAlbumGQLViaRestMutation(PhotoAlbum photoAlbum, String prefix)
      throws Exception {
    String pbParentId = getParentPBAlbumId(photoAlbum.getId());

    String jsonString =
        String.format(
            "{\"query\": \"mutation createAlbumDTP($title: String!, $parentAlbumId: String!){ createAlbum(title: $title, parentAlbumId: $parentAlbumId){ id }}\", \"variables\": {\"title\": \"%s\", \"parentAlbumId\": \"%s\"}}",
            prefix + photoAlbum.getName(), pbParentId);
    return RequestBody.create(MediaType.parse("application/json"), jsonString);
  }

  private String getParentPBAlbumId(String albumId) throws Exception {
    // for top level album parent is PB root album
    if (albumId.equals(jobId.toString())) {
      return getPbRootAlbumId();
    } else {
      try {
        return getPBAlbumId(albumId);
      } catch (Exception e) {
        // in case if pbAlbumId not found for current album, migrate photos to the top level album
        return getPBAlbumId(jobId.toString());
      }
    }
  }

  private String getPBAlbumId(String albumId) throws IOException, NullPointerException {
    return jobStore.findData(jobId, albumId, PhotobucketAlbum.class).getPbId();
  }

  private PhotobucketGQLResponse parseGQLResponse(Response response) throws IOException {
    if (response.body() != null) {
      return objectMapper.readValue(response.body().string(), PhotobucketGQLResponse.class);
    } else {
      throw new IOException("Empty response body was provided by GQL server");
    }
  }

  private String getPbRootAlbumId() throws Exception {
    // request if pbRootAlbumId was not requested yet
    if (pbRootAlbumId == null) {
      RequestBody requestBody =
          RequestBody.create(
              MediaType.parse("application/json"),
              "{\"query\": \"query getRootAlbumIdDTP{ getProfile{ defaultAlbum }}\"}");
      Function<Response, ProcessingResult> bodyTransformF =
          response -> {
            try {
              // get photobucket pbRootAlbumId from response
              pbRootAlbumId = parseGQLResponse(response).getRootAlbumId();
              return new ProcessingResult(pbRootAlbumId);
            } catch (IOException e) {
              return new ProcessingResult(e);
            }
          };

      // always fail in case of error, as unable to proceed without knowing pbRootId
      Function<Void, Boolean> conditionalExceptionF = v -> true;

      // fallback result is null, as unable to proceed without root
      System.out.println("Getting root album");

      ProcessingResult result =
          performGQLRequest(requestBody, bodyTransformF, conditionalExceptionF, null);
      return result.extractOrThrow();

    } else {
      return pbRootAlbumId;
    }
  }

  // TODO: get data from microservices
  private Boolean isUserOveStorage(long contentLength) {
    return false;
  }

  // TODO: get upload date and date taken if possible
  private String getUploadDate(PhotoModel photoModel, BufferedInputStream inputStream) {
    return null;
  }
}
