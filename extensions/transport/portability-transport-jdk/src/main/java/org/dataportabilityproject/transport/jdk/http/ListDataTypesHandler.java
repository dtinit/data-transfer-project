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
package org.dataportabilityproject.transport.jdk.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.dataportabilityproject.api.action.datatype.DataTypesAction;
import org.dataportabilityproject.api.launcher.TypeManager;
import org.dataportabilityproject.transport.jdk.http.HandlerUtils.HttpMethods;
import org.dataportabilityproject.types.client.datatype.DataTypes;
import org.dataportabilityproject.types.client.datatype.GetDataTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;

/** HttpHandler for the {@link DataTypesAction}. */
final class ListDataTypesHandler implements HttpHandler {

  public static final String PATH = "/_/listDataTypes";
  private static final Logger logger = LoggerFactory.getLogger(ListDataTypesHandler.class);
  private final DataTypesAction dataTypesAction;
  private final ObjectMapper objectMapper;

  @Inject
  ListDataTypesHandler(DataTypesAction listServicesAction, TypeManager typeManager) {
    this.dataTypesAction = listServicesAction;
    this.objectMapper = typeManager.getMapper();
  }

  /** Services the {@link DataTypesAction} via the {@link HttpExchange}. */
  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(HandlerUtils.validateRequest(exchange, HttpMethods.GET, PATH));
    logger.debug("received request: {}", exchange.getRequestURI());

    DataTypes actionResponse =
        dataTypesAction.handle(new GetDataTypes());

    // Set response as type json
    Headers headers = exchange.getResponseHeaders();
    headers.set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());

    // Send response
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), actionResponse);
  }

  /** Handles error response. TODO: Determine whether to return user facing error message here. */
  public void handleError(HttpExchange exchange) throws IOException {
    DataTypes response = new DataTypes(Collections.emptySet());
    // Mark the response as type Json and send
    exchange
        .getResponseHeaders()
        .set(CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), response);
  }
}
