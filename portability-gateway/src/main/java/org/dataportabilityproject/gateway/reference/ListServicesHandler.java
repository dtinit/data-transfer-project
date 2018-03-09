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
import org.dataportabilityproject.gateway.action.listservices.ListServicesAction;
import org.dataportabilityproject.gateway.action.listservices.ListServicesActionRequest;
import org.dataportabilityproject.gateway.action.listservices.ListServicesActionResponse;
import org.dataportabilityproject.gateway.reference.ReferenceApiUtils.HttpMethods;
import org.dataportabilityproject.spi.cloud.types.TypeManager;
import org.dataportabilityproject.types.client.transfer.ListServicesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** HttpHandler for the {@link ListServicesAction}. */
final class ListServicesHandler implements HttpHandler {

  public static final String PATH = "/_/listServices";
  private static final Logger logger = LoggerFactory.getLogger(ListServicesHandler.class);
  private final ListServicesAction listServicesAction;
  private final ObjectMapper objectMapper;

  @Inject
  ListServicesHandler(ListServicesAction listServicesAction, TypeManager typeManager) {
    this.listServicesAction = listServicesAction;
    this.objectMapper = typeManager.getMapper();
  }

  /** Services the {@link ListServicesAction} via the {@link HttpExchange}. */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(ReferenceApiUtils.validateRequest(exchange, HttpMethods.GET, PATH));
    logger.debug("received request: {}", exchange.getRequestURI());

    String transferDataType = ReferenceApiUtils.getRequestParams(exchange).get(JsonKeys.DATA_TYPE);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(transferDataType), "Missing data type");

    ListServicesActionRequest actionRequest = new ListServicesActionRequest(transferDataType);
    ListServicesActionResponse actionResponse = listServicesAction.handle(actionRequest);

    if (actionResponse.getErrorMsg() != null) {
      logger.warn("Error during action: {}", actionResponse.getErrorMsg());
      handleError(exchange, transferDataType);
      return;
    }

    // TODO: Determine if export and import services should remain separate in the response
    String[] services = actionResponse.getServices().toArray(new String[0]);
    ListServicesResponse response = new ListServicesResponse(transferDataType, services, services);

    // Set response as type json
    Headers headers = exchange.getResponseHeaders();
    headers.set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());

    // Send response
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), response);
  }

  /** Handles error response. TODO: Determine whether to return user facing error message here. */
  public void handleError(HttpExchange exchange, String transferDataType) throws IOException {
    String[] empty = new String[] {};
    ListServicesResponse response = new ListServicesResponse(transferDataType, empty, empty);
    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), response);
  }
}
