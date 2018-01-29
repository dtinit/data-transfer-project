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
package org.dataportabilityproject.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.types.client.transfer.ListServicesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_CONTENT_TYPE;

/**
 * HttpHandler for the ListServices service
 */
final class ListServicesHandler implements HttpHandler {

  public static final String PATH = "/_/listServices";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  private final Logger logger = LoggerFactory.getLogger(ListServicesHandler.class);
  private final ServiceProviderRegistry serviceProviderRegistry;

  @Inject
  ListServicesHandler(ServiceProviderRegistry serviceProviderRegistry) {
    this.serviceProviderRegistry = serviceProviderRegistry;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityApiUtils.validateRequest(exchange, HttpMethods.GET, PATH));

    // Set response as type json
    Headers headers = exchange.getResponseHeaders();
    headers.set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());

    String dataTypeParam = PortabilityApiUtils.getRequestParams(exchange)
        .get(JsonKeys.DATA_TYPE);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(dataTypeParam), "Missing data type");

    ListServicesResponse response = generateGetResponse(dataTypeParam);

    // Send response
    exchange.sendResponseHeaders(200, 0);
    objectMapper.writeValue(exchange.getResponseBody(), response);
  }

  private ListServicesResponse generateGetResponse(String dataTypeParam) {
    // Validate incoming data type parameter
    PortableDataType dataType = getDataType(dataTypeParam);

    List<String> exportServices = new ArrayList<String>();
    List<String> importServices = new ArrayList<String>();

    try {
      exportServices = serviceProviderRegistry.getServiceProvidersThatCanExport(dataType);
      importServices = serviceProviderRegistry.getServiceProvidersThatCanImport(dataType);
    } catch (Exception e) {
      logger.error("Encountered error with getServiceProviders ", e);
    }

    if (exportServices.isEmpty() || importServices.isEmpty()) {
      logger.warn("Empty service list found, export size: {}, import size: {}",
          exportServices.size(), importServices.size());
    }

    return new ListServicesResponse(dataTypeParam,
        exportServices.toArray(new String[exportServices.size()]),
        importServices.toArray(new String[importServices.size()]));


  }

  /**
   * Parse and validate the data type .
   */
  private PortableDataType getDataType(String dataType) {
    Optional<PortableDataType> dataTypeOption = Enums
        .getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type not found: %s", dataType);
    return dataTypeOption.get();
  }

}