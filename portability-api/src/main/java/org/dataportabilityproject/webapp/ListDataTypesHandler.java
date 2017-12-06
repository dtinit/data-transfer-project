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

import com.google.common.base.Preconditions;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonWriter;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.LogUtils;
import org.dataportabilityproject.shared.PortableDataType;

/**
 * HTTP Handler for the listDataTypes service
 */
public class ListDataTypesHandler implements HttpHandler {

  private final ServiceProviderRegistry serviceProviderRegistry;

  public ListDataTypesHandler(ServiceProviderRegistry serviceProviderRegistry) {
    this.serviceProviderRegistry = serviceProviderRegistry;
  }

  public void handle(HttpExchange exchange) throws IOException {
    Preconditions.checkArgument(
        PortabilityServerUtils.validateRequest(exchange, HttpMethods.GET, "/_/listDataTypes"),
        "This resource only supports GET.");

    // Mark the response as type Json
    Headers headers = exchange.getResponseHeaders();
    headers.set(HEADER_CONTENT_TYPE, "application/json; charset=" + StandardCharsets.UTF_8.name());

    JsonArrayBuilder builder = Json.createArrayBuilder();

    for (PortableDataType data_type : PortableDataType.values()) {
      try {
        if (hasImportAndExport(data_type)) {
          builder.add(data_type.name());
        }
      } catch (Exception e) {
        LogUtils.log("hasImportAndExport for datatype %s failed", data_type.name());
      }
    }

    exchange.sendResponseHeaders(200, 0);
    JsonWriter writer = Json.createWriter(exchange.getResponseBody());
    writer.writeArray(builder.build());
    writer.close();
  }

  /**
   * Returns whether or not the given {@link PortableDataType} has at least one registered service
   * that can import and at least one registered service that can export.
   */
  private boolean hasImportAndExport(PortableDataType type) throws Exception {
    return
        (serviceProviderRegistry.getServiceProvidersThatCanExport(type).size() > 0)
            &&
            (serviceProviderRegistry.getServiceProvidersThatCanImport(type).size() > 0);
  }

}