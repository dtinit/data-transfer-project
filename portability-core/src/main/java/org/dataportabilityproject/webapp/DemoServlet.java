
package org.dataportabilityproject.webapp;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
 
public class DemoServlet extends HttpServlet
{
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
      System.out.println("\n\nRequest: " + request);
      System.out.println("Request.getRequestURI(): " + request.getRequestURI());
      if (request.getRequestURI().contains("listServices")) {
        listServicesForDataTypes(request, response);
        return;
      }
      listDataTypes(request, response);
    }

    /** List the data types available. */
    private void listDataTypes(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
      JsonObject dataTypes = new JsonObject();
      dataTypes.addProperty("photos", "Photos");
      dataTypes.addProperty("tasks", "Tasks");
      dataTypes.addProperty("test", "Test");
      Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(gson.toJson(dataTypes));
    }

    /** List the services available for a particular data type. */
    private void listServicesForDataTypes(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

      // TODO: Wire up to Portability backend
      JsonObject exportServices = new JsonObject();
      exportServices.addProperty("google", "Google");
      exportServices.addProperty("microsoft", "Microsoft");
      exportServices.addProperty("test", "Test");

      JsonObject importServices = new JsonObject();
      importServices.addProperty("google", "Google");
      importServices.addProperty("microsoft", "Microsoft");
      importServices.addProperty("test", "Test");

      JsonObject services = new JsonObject();
      services.add("export", exportServices);
      services.add("import", importServices);

      Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
      response.setContentType("application/json");
      response.setCharacterEncoding("UTF-8");
      response.getWriter().write(gson.toJson(services));
    }

}
