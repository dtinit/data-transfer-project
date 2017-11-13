package org.dataportabilityproject.webapp;

import static org.apache.axis.transport.http.HTTPConstants.HEADER_CONTENT_TYPE;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.shared.PortableDataType;
import org.json.JSONArray;

public class ListDataTypesHandler implements HttpHandler {
  private ServiceProviderRegistry serviceProviderRegistry;

  public ListDataTypesHandler(ServiceProviderRegistry serviceProviderRegistry) {
    this.serviceProviderRegistry = serviceProviderRegistry;
  }

  public void handle(HttpExchange exchange) throws IOException {
    // TODO: check the METHOD received in the exchange - this only allows GET.
    // Mark the response as type Json
    Headers headers = exchange.getResponseHeaders();
    headers.set(HEADER_CONTENT_TYPE, "application/json; charset="+ StandardCharsets.UTF_8.name());

    List<String> data_types = new ArrayList<String>();

    for (PortableDataType data_type : PortableDataType.values()) {
      try {
        if (hasImportAndExport(data_type)) {
          data_types.add("\"" + data_type.name() + "\"");
        }
      } catch (Exception e) {
        System.err.println("hasImportAndExport for datatype failed");
      }
    }

    // generate the json for the data types. This is wonky and should be fixed - but using the
    // JsonArrayBuilder causes things to mess up and not sure why.
    String response = "[" + String.join(",", data_types) + "]";

    System.out.println("Response is: " + response);
    System.out.flush();

    exchange.sendResponseHeaders(200, 0);
    OutputStream responseBody = exchange.getResponseBody();
    responseBody.write(response.getBytes());
    responseBody.close();

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

