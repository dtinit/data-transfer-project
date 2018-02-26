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
package org.dataportabilityproject.gateway.reference;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.dataportabilityproject.gateway.action.createjob.CreateJobAction;
import org.dataportabilityproject.gateway.action.listdatatypes.ListDataTypesAction;
import org.dataportabilityproject.gateway.action.listdatatypes.ListDataTypesActionRequest;
import org.dataportabilityproject.gateway.action.listdatatypes.ListDataTypesActionResponse;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.HttpMethods;
import org.dataportabilityproject.spi.cloud.types.TypeManager;
import org.dataportabilityproject.types.client.transfer.ListDataTypesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for the {@link CreateJobAction}.
 */
final class ListDataTypesHandler implements HttpHandler {

  private static final Logger logger = LoggerFactory.getLogger(ListDataTypesHandler.class);
  public static final String PATH = "/_/listDataTypes";
  private final ListDataTypesAction listDataTypesAction;
  private final ObjectMapper objectMapper;

  @Inject
  ListDataTypesHandler(ListDataTypesAction listServicesAction, TypeManager typeManager) {
    this.listDataTypesAction = listServicesAction;
        this.objectMapper = typeManager.getMapper();
  }

  /**
   * Services the {@link ListDataTypesAction} via the {@link HttpExchange}.
   */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        ReferenceApiUtils.validateRequest(exchange, HttpMethods.GET, PATH));
    logger.debug("received request: {}", exchange.getRequestURI());

    String transferDataType = ReferenceApiUtils.getRequestParams(exchange)
        .get(JsonKeys.DATA_TYPE);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(transferDataType), "Missing data type");

    ListDataTypesActionResponse actionResponse = listDataTypesAction.handle(new ListDataTypesActionRequest());

    if (actionResponse.getErrorMsg() != null) {
      logger.warn("Error during action: {}", actionResponse.getErrorMsg());
      handleError(exchange, transferDataType);
      return;
    }

    String[] dataTypes = actionResponse.getTransferDataTypes().toArray(new String[0]);
    ListDataTypesResponse response = new ListDataTypesResponse(dataTypes);

    // Set response as type json
    Headers headers = exchange.getResponseHeaders();
    headers.set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());

    // Send response
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), response);
  }

  /** Handles error response. TODO: Determine whether to return user facing error message here. */
  public void handleError(HttpExchange exchange, String transferDataType) throws IOException {
    String[] empty = new String[]{};
    ListDataTypesResponse response = new ListDataTypesResponse(empty);
    // Mark the response as type Json and send
    exchange.getResponseHeaders()
        .set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), response);
  }
}
