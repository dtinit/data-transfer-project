package org.datatransferproject.transfer.solid;

import static com.google.common.base.Preconditions.checkState;

import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.collect.ImmutableList;
import com.google.re2j.Pattern;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.datatransferproject.transfer.solid.contacts.SolidContactsExport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolidUtilities {
  private static final Logger logger = LoggerFactory.getLogger(SolidContactsExport.class);
  private static final Pattern PROBLEMATIC_TURTLE = Pattern.compile("(\\s\\d+\\.)\n");
  private static final HttpTransport TRANSPORT = new NetHttpTransport();
  private final String authCookie;
  final HttpRequestFactory factory;

  public SolidUtilities(String authCookie) {
    this.authCookie = authCookie;
    this.factory = TRANSPORT.createRequestFactory();
  }

  public void explore(String url, Consumer<Resource> resourceConsumer) throws IOException {
    logger.debug("Exploring: {}", url);
    Model model = getModel(url);

    Resource selfResource = getResource(url, model);

    if (selfResource == null) {
      resourceConsumer.accept(model.createResource(url));
      return;
    }

    if (isType(selfResource, "http://www.w3.org/ns/ldp#Container")) {
      for (Resource r : getContainedResource(model, url)) {
        explore(r.getURI(), resourceConsumer);
      }
    }

    resourceConsumer.accept(selfResource);
  }

  public Model getModel(String url) throws IOException {
    HttpRequestFactory factory = TRANSPORT.createRequestFactory();

    HttpRequest rootGetRequest = factory.buildGetRequest(
        new GenericUrl(url));
    HttpHeaders headers = new HttpHeaders();
    headers.setCookie(authCookie);
    headers.setAccept("text/turtle");
    rootGetRequest.setHeaders(headers);

    HttpResponse response = rootGetRequest.execute();
    if (response.getStatusCode() != 200) {
      throw new IOException("Unexpected return code: "
          + response.getStatusCode()
          + "\nMessage:\n"
          + response.getStatusMessage());

    }
    StringWriter writer = new StringWriter();
    IOUtils.copy(response.getContent(), writer, "UTF-8");
    String fixedString = fixProblematicPeriods(writer.toString());
    Model defaultModel = ModelFactory.createDefaultModel();
    return defaultModel.read(
        new StringReader(fixedString),
        url,
        "TURTLE");
  }

  public static List<Statement> getProperties(Resource r, String property) {
    return r.listProperties()
        .filterKeep(s -> s.getPredicate().toString().equalsIgnoreCase(property))
        .toList();
  }

  public void recursiveDelete(String url) throws IOException{
    explore(url, r-> delete(r.getURI()));
  }

  public void delete(String url)  {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept("text/turtle");
    headers.setCookie(authCookie);

    try {
      HttpRequest deleteRequest = factory.buildDeleteRequest(new GenericUrl(url))
          .setThrowExceptionOnExecuteError(false);
      deleteRequest.setHeaders(headers);

      validateResponse(deleteRequest.execute(), 200);
      logger.debug("Deleted: {}", url);
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't delete: " + url, e);
    }
  }

  public String postContent(
      String url,
      String slug,
      String type,
      Resource resource)
      throws IOException {
    StringWriter stringWriter = new StringWriter();
    resource.getModel().write(stringWriter, "TURTLE");
    return postContent(url, slug, type, stringWriter.toString());
  }

  public String postContent(
      String url,
      String slug,
      String type,
      String contentString)
      throws IOException {
    HttpContent content = new ByteArrayContent("text/turtle", contentString.getBytes());

    HttpRequest postRequest = factory.buildPostRequest(
        new GenericUrl(url), content);
    HttpHeaders headers = new HttpHeaders();
    headers.setCookie(authCookie);
    headers.set("Link", "<" + type + ">; rel=\"type\"");
    headers.set("Slug", slug);
    postRequest.setHeaders(headers);

    HttpResponse response = postRequest.execute();

    validateResponse(response, 201);
    String location = response.getHeaders().getLocation();

    return location;
  }

  static void validateResponse(HttpResponse response, int expectedCode) throws IOException {
    if (response.getStatusCode() != expectedCode) {
      throw new IOException("Unexpected return code: "
          + response.getStatusCode()
          + "\nMessage:\n"
          + response.getStatusMessage()
          + "\nHeaders:\n"
          + response.getHeaders());

    }
  }

  static List<Resource> getContainedResource(Model model, String url) {
    ImmutableList.Builder<Resource> results = new ImmutableList.Builder<>();

    Resource self = model.getResource(url);
    self.listProperties(model.createProperty("http://www.w3.org/ns/ldp#contains"))
        .forEachRemaining(s-> results.add(s.getResource()));
    /*List<Statement> containedStatements = getProperties(
        self,
        "http://www.w3.org/ns/ldp#contains");

    for (Statement s : containedStatements) {
      results.add(s.getResource());
    }*/

    return results.build();
  }

  public static boolean isType(Resource resource, String type) {
    return resource.listProperties(RDF.type)
        .toList()
        .stream()
        .anyMatch(s -> s.getResource().getURI().equalsIgnoreCase(type));
  }

  public static Resource getResource(String url, Model model) {
    List<Resource> matchingSubjects = model.listSubjects()
        .filterKeep(s -> s.getURI() != null)
        .filterKeep(s -> s.getURI().equalsIgnoreCase(url)
            || s.getURI().equalsIgnoreCase(url + "#this")).toList();
    if (matchingSubjects.isEmpty()) {
      return null;
    }
    checkState(matchingSubjects.size() == 1,
        "Model %s didn't contain %s",
        model,
        url);
    return matchingSubjects.get(0);
  }

  static String fixProblematicPeriods(String source) {
    // SOLID outputs lines like:
    // st:size 4096.
    // And jena thinks the trailing '.' belongs to the number, and not the end of
    // the stanza.  So this turn cases like that into:
    // st:size 4096.0.
    // which everyone likes
    return PROBLEMATIC_TURTLE.matcher(source).replaceAll("$10.\n");
  }

  /** Utility method for debugging model problems. **/
  @SuppressWarnings("unused")
  public static void describeModel(Model model) {
    model.listSubjects().forEachRemaining(
        r -> {
          logger.info(r.toString());
          StmtIterator props = r.listProperties();
          props.forEachRemaining(p -> {
            logger.info("\t" + p.getPredicate() + " " + p.getObject());
          });
        }
    );
  }
}
