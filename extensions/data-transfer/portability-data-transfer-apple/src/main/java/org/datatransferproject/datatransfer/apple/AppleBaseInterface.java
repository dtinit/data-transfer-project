/*
 * Copyright 2023 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_INSUFFICIENT_STORAGE;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import org.apache.commons.lang3.NotImplementedException;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.DestinationNotFoundException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.UnconfirmedUserException;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * The Base Interface for all the {@link org.datatransferproject.types.common.models.DataVertical}s
 * that Apple Supports.
 */
public interface AppleBaseInterface {
    default String sendPostRequest(@Nonnull String url, @Nonnull final byte[] requestData) throws IOException, CopyExceptionWithFailureReason {
      throw new NotImplementedException("sendPostRequest is not implemented !! ");
    }

    default TokensAndUrlAuthData refreshTokens(final TokensAndUrlAuthData authData, final AppCredentials appCredentials, Monitor monitor)
        throws CopyExceptionWithFailureReason {

        final String refreshToken = authData.getRefreshToken();
        final String refreshUrlString = authData.getTokenServerEncodedUrl();
        final String clientId = appCredentials.getKey();
        final String clientSecret = appCredentials.getSecret();

        final Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("grant_type", "refresh_token");
        parameters.put("refresh_token", refreshToken);

        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            sj.add(entry.getKey() + "=" + entry.getValue());
        }

        final byte[] requestData = sj.toString().getBytes(StandardCharsets.ISO_8859_1);
        try {
            final String responseString = sendPostRequest(refreshUrlString, requestData);
            final JSONParser parser = new JSONParser();
            final JSONObject json = (JSONObject) parser.parse(responseString);
            final String accessToken = (String) json.get("access_token");


            monitor.debug(() -> "Successfully refreshed token");

            return new TokensAndUrlAuthData(accessToken, refreshToken, refreshUrlString);
        } catch (ParseException | IOException | CopyExceptionWithFailureReason e) {
            monitor.debug(() -> "Failed to refresh token", e);
            throw new InvalidTokenException("Unable to refresh token", e);
        }
    }

    default void convertAndThrowException(@NotNull final IOException e, @NotNull final HttpURLConnection con)
        throws IOException, CopyExceptionWithFailureReason {

        switch (con.getResponseCode()) {
            case SC_UNAUTHORIZED:
                throw new UnconfirmedUserException("Unauthorized iCloud User", e);
            case SC_PRECONDITION_FAILED:
                throw new PermissionDeniedException("Permission Denied", e);
            case SC_NOT_FOUND:
                throw new DestinationNotFoundException("iCloud Photos Library not found", e);
            case SC_INSUFFICIENT_STORAGE:
                throw new DestinationMemoryFullException("iCloud Storage is full", e);
            case SC_SERVICE_UNAVAILABLE:
                throw new IOException("DTP import service unavailable", e);
            case SC_BAD_REQUEST:
                throw new IOException("Bad request sent to iCloud Photos import api", e);
            case SC_INTERNAL_SERVER_ERROR:
                throw new IOException("Internal server error in iCloud Photos service", e);
            case SC_OK:
                break;
            default:
                throw e;
        }
    }
}
