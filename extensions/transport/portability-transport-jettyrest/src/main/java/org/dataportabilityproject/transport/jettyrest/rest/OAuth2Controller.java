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
package org.dataportabilityproject.transport.jettyrest.rest;

import org.dataportabilityproject.api.action.Action;
import org.dataportabilityproject.types.client.transfer.GenerateServiceAuthData;
import org.dataportabilityproject.types.client.transfer.ServiceAuthData;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.dataportabilityproject.transport.jettyrest.rest.RESTConstants.EXPORT_AUTH_DATA_KEY;
import static org.dataportabilityproject.transport.jettyrest.rest.RESTConstants.IMPORT_AUTH_DATA_KEY;
import static org.dataportabilityproject.types.client.transfer.GenerateServiceAuthData.Mode.EXPORT;
import static org.dataportabilityproject.types.client.transfer.GenerateServiceAuthData.Mode.IMPORT;

/**
 * Receives callbacks from OAuth export and import services. The OAuth token is retrieved and a
 * request to generate encrypted auth data is issued. The auth data is returned as a cookie to the
 * client.
 */
@Path("callback")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class OAuth2Controller {
  private final String baseUrl;
  private Action<GenerateServiceAuthData, ServiceAuthData> action;

  public OAuth2Controller(String baseUrl, Action<GenerateServiceAuthData, ServiceAuthData> action) {
    this.baseUrl = baseUrl;
    this.action = action;
  }

  @GET
  public Response handleCallback(
      @QueryParam("state") String id,
      @QueryParam("code") String token,
      @QueryParam("error") String error,
      @CookieParam(EXPORT_AUTH_DATA_KEY) Cookie exportCookie) {

    // check for error
    if (error != null || token == null || id == null) {
      // TODO log
      return Response.status(Response.Status.SEE_OTHER).location(URI.create("/error")).build();
    }
    GenerateServiceAuthData.Mode mode = exportCookie == null ? EXPORT : IMPORT;
    ServiceAuthData data = action.handle(new GenerateServiceAuthData(id, token, mode));

    String cookieName = mode == EXPORT ? EXPORT_AUTH_DATA_KEY : IMPORT_AUTH_DATA_KEY;
    NewCookie authCookie = new NewCookie(cookieName, data.getAuthData());

    String redirect = baseUrl + (mode == EXPORT ? "/import" : "/start");

    return Response.status(Response.Status.SEE_OTHER)
        .location(URI.create(redirect))
        .cookie(authCookie)
        .build();
  }
}
