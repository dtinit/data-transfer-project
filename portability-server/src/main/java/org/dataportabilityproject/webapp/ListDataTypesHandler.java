package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_CONTENT_TYPE;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonWriter;
import org.dataportabilityproject.ServiceProviderRegistry;
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
    // This handler only supports the GET method.
    if(!PortabilityServerUtils.ValidateGetRequest(exchange)){
      return;
    }

    // Mark the response as type Json
    Headers headers = exchange.getResponseHeaders();
    headers.set(HEADER_CONTENT_TYPE, "application/json; charset="+ StandardCharsets.UTF_8.name());

    JsonArrayBuilder builder = Json.createArrayBuilder();

    for (PortableDataType data_type : PortableDataType.values()) {
      try {
        if (hasImportAndExport(data_type)) {
          builder.add(data_type.name());
        }
      } catch (Exception e) {
        System.err.println("hasImportAndExport for datatype failed");
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