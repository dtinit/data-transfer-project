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

import static org.apache.axis.transport.http.HTTPConstants.HEADER_CONTENT_TYPE;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HttpHandler for the ListServices service
 */
final class ListServicesHandler implements HttpHandler {
  private final Logger logger = LoggerFactory.getLogger(ListServicesHandler.class);
  private final ServiceProviderRegistry serviceProviderRegistry;

  @Inject
  ListServicesHandler(ServiceProviderRegistry serviceProviderRegistry) {
    this.serviceProviderRegistry = serviceProviderRegistry;
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityApiUtils.validateRequest(exchange, HttpMethods.GET, "/_/listServices"));

    // Set response as type json
    Headers headers = exchange.getResponseHeaders();
    headers.set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());

    String dataTypeParam = PortabilityApiUtils.getRequestParams(exchange)
        .get(JsonKeys.DATA_TYPE);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(dataTypeParam), "Missing data type");

    JsonObject response = generateGetResponse(dataTypeParam);

    // Send response
    exchange.sendResponseHeaders(200, 0);
    JsonWriter writer = Json.createWriter(exchange.getResponseBody());
    writer.write(response);
    writer.close();
  }

  private JsonObject generateGetResponse(String dataTypeParam) {
    // Validate incoming data type parameter
    PortableDataType dataType = getDataType(dataTypeParam);
    JsonObjectBuilder builder = Json.createObjectBuilder();

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

    // Construct Json.
    JsonArrayBuilder exportBuilder = Json.createArrayBuilder();
    for (String s : exportServices) {
      exportBuilder.add(s);
    }
    JsonArrayBuilder importBuilder = Json.createArrayBuilder();
    for (String s : importServices) {
      importBuilder.add(s);
    }

    return builder.add(JsonKeys.EXPORT, exportBuilder).add(JsonKeys.IMPORT, importBuilder).build();
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