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
import ezvcard.io.json.JCardReader;
import okhttp3.OkHttpClient;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.microsoft.transformer.TransformResult;
import org.datatransferproject.transfer.microsoft.transformer.TransformerService;
import org.datatransferproject.types.transfer.auth.TokenAuthData;
import org.datatransferproject.types.common.models.contacts.ContactsModelWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.datatransferproject.transfer.microsoft.common.RequestHelper.batchRequest;
import static org.datatransferproject.transfer.microsoft.common.RequestHelper.createRequest;

/**
 * Performs a batch import of contacts using the Microsoft Graph API. For details see:
 * https://developer.microsoft.com/en-us/graph/docs/concepts/json_batching.
 */
public class MicrosoftContactsImporter implements Importer<TokenAuthData, ContactsModelWrapper> {
  private static final String CONTACTS_URL = "/me/contacts"; // must be relative for batch operations

  private final String baseUrl;
  private final OkHttpClient client;
  private final ObjectMapper objectMapper;
  private final TransformerService transformerService;

  public MicrosoftContactsImporter(
      String baseUrl,
      OkHttpClient client,
      ObjectMapper objectMapper,
      TransformerService transformerService) {
    this.baseUrl = baseUrl;
    this.client = client;
    this.objectMapper = objectMapper;
    this.transformerService = transformerService;
  }

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentImportExecutor,
      TokenAuthData authData,
      ContactsModelWrapper wrapper) {
    JCardReader reader = new JCardReader(wrapper.getVCards());
    try {
      List<VCard> cards = reader.readAll();

      List<String> problems = new ArrayList<>();

      int[] id = new int[] {1};
      List<Map<String, Object>> requests =
          cards
              .stream()
              .map(
                  card -> {
                    TransformResult<LinkedHashMap> result =
                        transformerService.transform(LinkedHashMap.class, card);
                    problems.addAll(result.getProblems());
                    LinkedHashMap contact = result.getTransformed();
                    Map<String, Object> request = createRequest(id[0], CONTACTS_URL, contact);
                    id[0]++;
                    return request;
                  })
              .collect(toList());

      if (!problems.isEmpty()) {
        // TODO log problems
      }

      return batchRequest(authData, requests, baseUrl, client, objectMapper).getResult();
    } catch (IOException e) {
      // TODO log
      e.printStackTrace();
      return new ImportResult(e);
    }
  }
}
