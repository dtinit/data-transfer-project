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
package org.datatransferproject.transfer.microsoft.contacts;

import com.fasterxml.jackson.databind.ObjectMapper;
import ezvcard.VCard;
import ezvcard.io.json.JCardWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.transfer.microsoft.transformer.TransformResult;
import org.datatransferproject.transfer.microsoft.transformer.TransformerService;
import org.datatransferproject.transfer.microsoft.types.GraphPagination;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.common.models.contacts.ContactsModelWrapper;

/**
 * Exports Microsoft contacts using the Graph API.
 */
public class MicrosoftContactsExporter implements Exporter<TokensAndUrlAuthData, ContactsModelWrapper> {

  private static final String CONTACTS_SUBPATH = "/v1.0/me/contacts";
  private static final String ODATA_NEXT = "@odata.nextLink";

  private final String contactsUrl;
  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TransformerService transformerService;

  public MicrosoftContactsExporter(
      String baseUrl,
      OkHttpClient client,
      ObjectMapper objectMapper,
      TransformerService transformerService) {
    this.contactsUrl = baseUrl + CONTACTS_SUBPATH;
    this.client = client;
    this.objectMapper = objectMapper;
    this.transformerService = transformerService;
  }

  @Override
  public ExportResult<ContactsModelWrapper> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation) {
    GraphPagination graphPagination =
        exportInformation.isPresent() ? (GraphPagination) exportInformation.get()
            .getPaginationData() : null;
    if (graphPagination != null && graphPagination.getNextLink() != null) {
      return doExport(authData, graphPagination.getNextLink());
    } else {
      return doExport(authData, contactsUrl);
    }
  }

  @SuppressWarnings("unchecked")
  private ExportResult<ContactsModelWrapper> doExport(TokensAndUrlAuthData authData, String url) {
    Request.Builder graphReqBuilder = new Request.Builder().url(url);
    graphReqBuilder.header("Authorization", "Bearer " + authData.getAccessToken());

    try (Response graphResponse = client.newCall(graphReqBuilder.build()).execute()) {
      ResponseBody body = graphResponse.body();
      if (body == null) {
        return new ExportResult<>(new Exception( "Error retrieving contacts: response body was null"));
      }
      String graphBody = new String(body.bytes());
      Map graphMap = objectMapper.reader().forType(Map.class).readValue(graphBody);

      String nextLink = (String) graphMap.get(ODATA_NEXT);
      ContinuationData continuationData =
          nextLink == null ? null : new ContinuationData(new GraphPagination(nextLink));

      List<Map<String, Object>> rawContacts = (List<Map<String, Object>>) graphMap.get("value");
      if (rawContacts == null) {
        return new ExportResult<>(ExportResult.ResultType.END);
      }

      ContactsModelWrapper wrapper = transform(rawContacts);
      return new ExportResult<>(ExportResult.ResultType.CONTINUE, wrapper, continuationData);
    } catch (IOException e) {
      e.printStackTrace(); // FIXME log error
      return new ExportResult<>(e);
    }
  }

  private ContactsModelWrapper transform(List<Map<String, Object>> rawContacts) {
    StringWriter stringWriter = new StringWriter();
    try (JCardWriter writer = new JCardWriter(stringWriter)) {
      for (Map<String, Object> rawContact : rawContacts) {
        TransformResult<VCard> result = transformerService.transform(VCard.class, rawContact);
        if (result.hasProblems()) {
          // discard
          // FIXME log problem
          continue;
        }
        writer.write(result.getTransformed());
      }
    } catch (IOException e) {
      // TODO log
      e.printStackTrace();
      return new ContactsModelWrapper("");
    }
    return new ContactsModelWrapper(stringWriter.toString());
  }
}
