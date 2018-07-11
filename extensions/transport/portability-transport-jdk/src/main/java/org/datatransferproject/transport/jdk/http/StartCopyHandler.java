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
package org.datatransferproject.transport.jdk.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.HttpHeaders;
import org.datatransferproject.api.action.startjob.StartJobAction;
import org.datatransferproject.api.action.startjob.StartJobActionRequest;
import org.datatransferproject.api.action.startjob.StartJobActionResponse;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.spi.api.token.TokenManager;
import org.datatransferproject.types.client.transfer.DataTransferResponse;
import org.datatransferproject.types.client.transfer.DataTransferResponse.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class StartCopyHandler implements HttpHandler {
  public static final String PATH = "/_/startCopy";

  private final StartJobAction startJobAction;
  private final TokenManager tokenManager;
  private final ObjectMapper objectMapper;

  @Inject
  StartCopyHandler(
      StartJobAction startJobAction, TokenManager tokenManager, TypeManager typeManager) {
    this.startJobAction = startJobAction;
    this.tokenManager = tokenManager;
    this.objectMapper = typeManager.getMapper();
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        HandlerUtils.validateRequest(exchange, HandlerUtils.HttpMethods.POST, PATH));

    UUID jobId = HandlerUtils.validateJobId(exchange.getRequestHeaders(), tokenManager);

    //  Validate auth data is present in cookies
    String exportAuthCookieValue =
        HandlerUtils.getCookie(
            exchange.getRequestHeaders(), JsonKeys.EXPORT_AUTH_DATA_COOKIE_KEY);
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(exportAuthCookieValue), "Export auth cookie required");

    String importAuthCookieValue =
        HandlerUtils.getCookie(
            exchange.getRequestHeaders(), JsonKeys.IMPORT_AUTH_DATA_COOKIE_KEY);
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(importAuthCookieValue), "Import auth cookie required");

    // We have the data, now update to 'pending transfer worker assignment' so a transfer worker may
    // be assigned
    StartJobActionRequest request =
        new StartJobActionRequest(jobId, exportAuthCookieValue, importAuthCookieValue);
    StartJobActionResponse response = startJobAction.handle(request);

    // TODO: Determine if we need more fields populated or a new object
    DataTransferResponse dataTransferResponse =
        new DataTransferResponse(
            "", // job.exportService(),
            "", // job.importService(),
            "", // job.transferDataType(),
            Status.INPROCESS,
            "" // FrontendConstantUrls.URL_COPY_PAGE);
            );

    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(
            HttpHeaders.CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);

    objectMapper.writeValue(exchange.getResponseBody(), dataTransferResponse);
  }
}
