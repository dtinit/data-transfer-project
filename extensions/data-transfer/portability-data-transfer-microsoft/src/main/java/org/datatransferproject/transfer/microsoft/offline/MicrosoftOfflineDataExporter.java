/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.datatransferproject.transfer.microsoft.offline;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.transfer.microsoft.spi.types.MicrosoftOfflineData;
import org.datatransferproject.types.transfer.auth.TokenAuthData;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Exports a offline data file.
 *
 * <p>This implementation expects the offline data to be a file named {@link #OFFLINE_DATA} placed
 * in the user's OneDrive in the folder [root]/offlinedata.
 *
 * <p>Note the format of the exported contents are opaque; they may change without notice.
 */
public class MicrosoftOfflineDataExporter implements Exporter<TokenAuthData, MicrosoftOfflineData> {
  private static final String OFFLINE_DATA = "offlinedata.json";

  private final String offlineDataUrl;
  private final String contentTemplate;
  private final OkHttpClient client;
  private final ObjectMapper objectMapper;

  public MicrosoftOfflineDataExporter(
      String baseUrl, OkHttpClient client, ObjectMapper objectMapper) {
    offlineDataUrl = baseUrl + "/v1.0/me/drive/root:/offlinedata:/children";
    contentTemplate = baseUrl + "/v1.0/me/drive/items/%s/content";
    this.client = client;
    this.objectMapper = objectMapper;
  }

  @Override
  public ExportResult<MicrosoftOfflineData> export(
      UUID jobId, TokenAuthData authData, Optional<ExportInformation> exportInformation) {

    try {
      String data = getDataFile(jobId, offlineDataUrl, authData);
      MicrosoftOfflineData offlineData = new MicrosoftOfflineData(data);
      return new ExportResult<>(ExportResult.ResultType.END, offlineData);
    } catch (IOException e) {
      e.printStackTrace(); // FIXME log error
      return new ExportResult<>(e);
    }
  }

  @SuppressWarnings("unchecked")
  private String getDataFile(UUID jobId, String url, TokenAuthData authData) throws IOException {
    Request.Builder requestBuilder = new Request.Builder().url(url);
    requestBuilder.header("Authorization", "Bearer " + authData.getToken());

    try (Response folderResponse = client.newCall(requestBuilder.build()).execute()) {
      ResponseBody folderBody = folderResponse.body();
      if (folderBody == null) {
        return "";
      }

      String folderContent = new String(folderBody.bytes());

      Map filesMap = objectMapper.reader().forType(Map.class).readValue(folderContent);

      String id = parseOfflineDataId(filesMap);
      if (id == null) {
        return "";
      }
      String contentUrl = String.format(contentTemplate, id);

      Request.Builder requestBuilder2 = new Request.Builder().url(contentUrl);
      requestBuilder2.header("Authorization", "Bearer " + authData.getToken());

      try (Response contentResponse = client.newCall(requestBuilder2.build()).execute()) {
        ResponseBody contentBody = contentResponse.body();
        if (contentBody == null) {
          return "";
        }
        return contentBody.string();

      } catch (IOException e) {
        // skip the offline data
        e.printStackTrace(); // FIXME log error
        return "";
      }
    }
  }

  @SuppressWarnings("unchecked")
  private String parseOfflineDataId(Map filesMap) {
    List<Map<String, Object>> items = (List<Map<String, Object>>) filesMap.get("value");
    if (items != null) {
      for (Map<String, Object> item : items) {
        String name = (String) item.get("name");
        if (OFFLINE_DATA.equals(name)) {
          return (String) item.get("id");
        }
      }
    }
    return null;
  }
}
