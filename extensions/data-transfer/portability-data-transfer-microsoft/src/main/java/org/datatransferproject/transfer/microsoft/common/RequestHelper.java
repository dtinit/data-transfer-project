/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.datatransferproject.transfer.microsoft.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.types.transfer.auth.TokenAuthData;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Provides request operations. */
public class RequestHelper {
  private static final String BATCH_URL = "/beta/$batch";

  private RequestHelper() {}

  /**
   * Creates a Graph API request object with required headers.
   *
   * @param id the request id
   * @param url the request URL
   * @param data the data
   */
  public static Map<String, Object> createRequest(int id, String url, LinkedHashMap data) {
    Map<String, Object> request = new LinkedHashMap<>();
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    request.put("headers", headers);
    request.put("id", id + "");
    request.put("method", "POST");
    request.put("url", url);
    request.put("body", data);
    return request;
  }

  /**
   * Creates a Graph API batch request with required the authorization header.
   *
   * @param authData the auth token
   * @param requests the batch request data
   * @param client the client to construct the request with
   * @param objectMapper the mapper to serialize data
   */
  @SuppressWarnings("unchecked")
  public static BatchResponse batchRequest(
      TokenAuthData authData,
      List<Map<String, Object>> requests,
      String baseUrl,
      OkHttpClient client,
      ObjectMapper objectMapper) {
    try {
      Map<String, Object> batch = new LinkedHashMap<>();
      batch.put("requests", requests);

      Request.Builder requestBuilder = new Request.Builder().url(baseUrl + BATCH_URL);
      requestBuilder.header("Authorization", "Bearer " + authData.getToken());
      requestBuilder.post(
          RequestBody.create(
              MediaType.parse("application/json"), objectMapper.writeValueAsString(batch)));
      try (Response response = client.newCall(requestBuilder.build()).execute()) {
        int code = response.code();
        if (code >= 200 && code <= 299) {
          ResponseBody body = response.body();
          if (body == null) {
            // FIXME evaluate HTTP response and return whether to retry
            return new BatchResponse(new ImportResult(ImportResult.ResultType.ERROR));
          }
          Map<String, Object> responseData = objectMapper.readValue(body.bytes(), Map.class);
          return new BatchResponse(
              new ImportResult(ImportResult.ResultType.OK),
              (List<Map<String, Object>>) responseData.get("responses"));
        } else {
          // FIXME evaluate HTTP response and return whether to retry
          return new BatchResponse(new ImportResult(ImportResult.ResultType.ERROR));
        }
      }
    } catch (IOException e) {
      // TODO log
      e.printStackTrace();
      return new BatchResponse(new ImportResult(e));
    }
  }

  public static class BatchResponse {
    private final ImportResult result;
    private final List<Map<String, Object>> batchResponse;

    public BatchResponse(ImportResult result, List<Map<String, Object>> batchResponse) {
      this.result = result;
      this.batchResponse = batchResponse;
    }

    public BatchResponse(ImportResult result) {
      this.result = result;
      this.batchResponse = null;
    }

    public ImportResult getResult() {
      return result;
    }

    public List<Map<String, Object>> getBatchResponse() {
      return batchResponse;
    }
  }
}
