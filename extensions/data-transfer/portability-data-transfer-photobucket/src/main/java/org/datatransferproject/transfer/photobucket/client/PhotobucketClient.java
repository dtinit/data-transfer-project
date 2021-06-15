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
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.transfer.photobucket.data.PhotobucketAlbum;
import org.datatransferproject.transfer.photobucket.data.response.gql.PhotobucketGQLResponse;
import org.datatransferproject.transfer.photobucket.data.ProcessingResult;
import org.datatransferproject.types.common.models.photos.PhotoAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;

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
      jobStore.findData(jobId, photoAlbum.getId(), PhotobucketAlbum.class);
    } catch (IOException ioException) {
      // if album was not created
      // generate gql query for getting pb album id via rest call
      String query = createAlbumGQLViaRestMutation(photoAlbum, namePrefix);
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

      ProcessingResult result =
          performGQLRequest(query, bodyTransformF, isTopLevelAlbumF, fallbackResult);
      result.extractOrThrow();
    }
  }

  public void uploadPhoto(PhotoModel photoModel) {}

  private ProcessingResult performGQLRequest(
      String query,
      Function<Response, ProcessingResult> responseTransformF,
      Function<Void, Boolean> conditionalExceptionF,
      ProcessingResult fallbackResult)
      throws IOException {

    // create builder for graphQL request
    Request.Builder gqlRequestBuilder = new Request.Builder().url(GQL_URL);
    // add authorization headers
    gqlRequestBuilder
        .header(AUTHORIZATION_HEADER, bearer)
        .header(CORRELATION_ID_HEADER, jobId.toString());
    FormBody.Builder bodyBuilder = new FormBody.Builder().add("query", query);
    gqlRequestBuilder.post(bodyBuilder.build());
    // create album
    Response createAlbumResponse = httpClient.newCall(gqlRequestBuilder.build()).execute();
    // gql server always provides 200 response code. If not, interrupt job
    if (createAlbumResponse.code() == 200) {
      try {
        // apply transformation function to the response
        return responseTransformF.apply(createAlbumResponse);
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
              createAlbumResponse.code(), jobId));
    }
  }

  private String createAlbumGQLViaRestMutation(PhotoAlbum photoAlbum, String prefix) throws Exception {
    String pbParentId = getParentPBAlbumId(photoAlbum.getId());

    return String.format(
        "mutation createAlbum {  createAlbum(title: \"%s\", parentAlbumId: \"%s\"){ id }}",
        prefix + photoAlbum.getName(), pbParentId);
  }

  private String getParentPBAlbumId(String albumId) throws Exception {
    // for top level album parent is PB root album
    if (albumId.equals(jobId.toString())) {
      return getPbRootAlbumId();
    } else {
      try {
        return jobStore.findData(jobId, albumId, PhotobucketAlbum.class).getPbId();
      } catch (Exception e) {
        // in case if pbAlbumId not found for current album, migrate photos to the top level album
        return jobStore.findData(jobId, jobId.toString(), PhotobucketAlbum.class).getPbId();
      }
    }
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
      String query = "query getRootAlbumId { getProfile { defaultAlbum } }";
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
      ProcessingResult result =
          performGQLRequest(query, bodyTransformF, conditionalExceptionF, null);
      return result.extractOrThrow();

    } else {
      return pbRootAlbumId;
    }
  }
}
