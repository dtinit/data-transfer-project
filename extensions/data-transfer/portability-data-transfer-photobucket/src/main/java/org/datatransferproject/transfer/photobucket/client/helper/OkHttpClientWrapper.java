package org.datatransferproject.transfer.photobucket.client.helper;

import com.google.api.client.auth.oauth2.Credential;
import okhttp3.*;
import org.datatransferproject.transfer.photobucket.model.ProcessingResult;
import org.datatransferproject.transfer.photobucket.model.error.WrongStatusCodeException;
import org.datatransferproject.transfer.photobucket.model.error.WrongStatusCodeRetriableException;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Function;

import static org.datatransferproject.transfer.photobucket.data.PhotobucketConstants.*;

public class OkHttpClientWrapper {
  private final String bearer;
  private final OkHttpClient httpClient;
  private final UUID jobId;

  public OkHttpClientWrapper(UUID jobId, Credential credential, OkHttpClient httpClient) {
    this.bearer = "Bearer " + credential.getAccessToken();
    this.httpClient = httpClient;
    this.jobId = jobId;
  }

  public ProcessingResult performGQLRequest(
      RequestBody requestBody,
      Function<Response, ProcessingResult> responseTransformF,
      Function<Void, Boolean> conditionalExceptionF,
      ProcessingResult fallbackResult)
      throws Exception {

    // create builder for graphQL request
    Request.Builder gqlRequestBuilder = new Request.Builder().url(GQL_URL);
    // add authorization headers
    gqlRequestBuilder
        .header(AUTHORIZATION_HEADER, bearer)
        .header(CORRELATION_ID_HEADER, jobId.toString())
        .header(REFERER_HEADER, REFERER_HEADER_VALUE)
        .header(ORIGIN_HEADER, ORIGIN_HEADER_VALUE);
    gqlRequestBuilder.post(requestBody);
    // post request
    Response response = httpClient.newCall(gqlRequestBuilder.build()).execute();
    // gql server always provides 200 response code. If not, interrupt job
    if (response.code() == 200) {
      try {
        // apply transformation function to the response
        return responseTransformF.apply(response);
      } catch (Exception e) {
        // throw error only for given rules
        if (conditionalExceptionF.apply(null)) {
          throw e;
        } else {
          return fallbackResult;
        }
      }
    } else if (response.code() >= 500) {
      throw new WrongStatusCodeRetriableException("GQL server provided response code >= 500.");
    } else {
      throw new WrongStatusCodeException(
          String.format(
              "Wrong status code=[%s] provided by GQL server for jobId=[%s]",
              response.code(), jobId));
    }
  }

  public ProcessingResult performRESTGetRequest(
      String url, Function<Response, ProcessingResult> responseTransformF) throws Exception {
    return performRESTRequest(url, null, "get", 200, responseTransformF);
  }

  public ProcessingResult performRESTPostRequest(
      String url, RequestBody requestBody, Function<Response, ProcessingResult> responseTransformF)
      throws Exception {
    return performRESTRequest(url, requestBody, "post", 201, responseTransformF);
  }

  private ProcessingResult performRESTRequest(
      String url,
      RequestBody requestBody,
      String method,
      int code,
      Function<Response, ProcessingResult> responseTransformF)
      throws Exception {
    Request.Builder uploadRequestBuilder = new Request.Builder().url(url);
    // add authorization headers
    uploadRequestBuilder
        .header(AUTHORIZATION_HEADER, bearer)
        .header(CORRELATION_ID_HEADER, jobId.toString());
    if (method.equals("post")) {
      uploadRequestBuilder.post(requestBody);
    } else {
      uploadRequestBuilder.get();
    }
    Response uploadImageResponse = httpClient.newCall(uploadRequestBuilder.build()).execute();
    if (uploadImageResponse.code() == code) {
      return responseTransformF.apply(uploadImageResponse);
    } else if (uploadImageResponse.code() >= 500) {
      throw new WrongStatusCodeRetriableException("REST server provided response code >= 500.");
    } else {
      // throw error in case upload was not successful
      throw new WrongStatusCodeException(
          String.format(
              "Wrong status code=[%s], message=[%s] provided by REST for jobId=[%s]",
              uploadImageResponse.code(), uploadImageResponse.message(), jobId));
    }
  }
}
