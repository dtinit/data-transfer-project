/*
 * Copyright 2024 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.apple.music;

import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleBaseInterface;
import org.datatransferproject.datatransfer.apple.constants.AppleMusicConstants;
import org.datatransferproject.datatransfer.apple.constants.AuditKeys;
import org.datatransferproject.datatransfer.apple.constants.Headers;
import org.datatransferproject.datatransfer.apple.exceptions.AppleHttpException;
import org.datatransferproject.datatransfer.apple.music.data.converters.AppleMusicPlaylistConverter;
import org.datatransferproject.datatransfer.apple.music.musicproto.MusicProtocol;
import org.datatransferproject.datatransfer.apple.music.musicproto.MusicProtocol.ImportMusicPlaylistsRequest;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.UnconfirmedUserException;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.common.models.music.MusicPlaylist;
import org.datatransferproject.types.common.models.music.MusicPlaylistItem;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INSUFFICIENT_STORAGE;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

/**
 * Apple Music implementation for the AppleBaseInterface.
 * This contains the internal handling components for processing and sending up the playlists and playlist-tracks to
 * Apple Music Data Transfer services.
 */
public class AppleMusicInterface implements AppleBaseInterface {

    protected String baseUrl;
    protected AppCredentials appCredentials;
    protected String exportingService;
    protected Monitor monitor;
    protected TokensAndUrlAuthData authData;

    public static final class API {
        public static final class V1 {
            public static final String PLAYLISTS = "/api/v1/playlists";
            public static final String TRACKS = "/api/v1/tracks";
        }
    }

    public AppleMusicInterface(
            @Nonnull final TokensAndUrlAuthData authData,
            @Nonnull final AppCredentials appCredentials,
            @Nonnull final String exportingService,
            @Nonnull final Monitor monitor) {
        this.authData = authData;
        this.appCredentials = appCredentials;
        this.exportingService = exportingService;
        this.monitor = monitor;
        this.baseUrl = "https://datatransfer.apple.com/music";
    }


    public int importPlaylists(final UUID jobId,
            IdempotentImportExecutor idempotentImportExecutor,
            Collection<MusicPlaylist> musicPlaylists) throws Exception {

        UnmodifiableIterator<List<MusicPlaylist>> batches = Iterators.partition(musicPlaylists.iterator(), AppleMusicConstants.MAX_NEW_PLAYLIST_REQUESTS);
        AtomicInteger successPlaylistsCount = new AtomicInteger(0);


        while (batches.hasNext()) {
            MusicProtocol.ImportMusicPlaylistsResponse importMusicPlaylistsResponse = importPlaylistsBatch(jobId.toString(), batches.next());

            for (MusicProtocol.MusicPlaylistResponse playlistResponse : importMusicPlaylistsResponse.getMusicPlaylistResponseList()) {
                if (playlistResponse.hasStatus() && playlistResponse.getStatus().getCode() == SC_OK) {
                    successPlaylistsCount.getAndIncrement();
                    idempotentImportExecutor.executeAndSwallowIOExceptions(
                            playlistResponse.getId(),
                            playlistResponse.getName(),
                            () -> {
                                monitor.debug(
                                        () -> "Apple importing music playlist",
                                        AuditKeys.jobId, jobId,
                                        AuditKeys.playlistId, playlistResponse.getId());
                                return playlistResponse.getId();
                            });
                } else {
                    idempotentImportExecutor.executeAndSwallowIOExceptions(
                            playlistResponse.getId(),
                            playlistResponse.getName(),
                            () -> {
                                monitor.severe(
                                        () -> "Error importing playlist: ",
                                        AuditKeys.jobId, jobId,
                                        AuditKeys.playlistId, playlistResponse.getId());

                                throw new IOException(
                                        String.format(
                                                "Failed to create playlist, error code: %d",
                                                playlistResponse.getStatus().getCode()));
                            });
                }
            }
        }
        return successPlaylistsCount.get();
    }

    public MusicProtocol.ImportMusicPlaylistsResponse importPlaylistsBatch(final String jobId, List<MusicPlaylist> musicPlaylistsBatch)
            throws IOException, CopyExceptionWithFailureReason, URISyntaxException {
        final ImportMusicPlaylistsRequest.Builder requestBuilder = ImportMusicPlaylistsRequest.newBuilder();

        List<MusicProtocol.MusicPlaylist> convertedMusicPlaylists = AppleMusicPlaylistConverter.convertToAppleMusicPlaylist(musicPlaylistsBatch);
        requestBuilder.setImportSessionId(jobId);
        requestBuilder.addAllMusicPlaylist(convertedMusicPlaylists);
        if (JobMetadata.getExportService() != null) {
            requestBuilder.setExportService(JobMetadata.getExportService());
        }

        final byte[] responseBody = sendPostRequest(baseUrl + API.V1.PLAYLISTS, requestBuilder.build().toByteArray());

        return MusicProtocol.ImportMusicPlaylistsResponse.parseFrom(responseBody);
    }


    public int importMusicPlaylistItems(
            final UUID jobId,
            IdempotentImportExecutor idempotentImportExecutor,
            List<MusicPlaylistItem> musicPlaylistItems) throws Exception {

        UnmodifiableIterator<List<MusicPlaylistItem>> batches = Iterators.partition(musicPlaylistItems.iterator(), AppleMusicConstants.MAX_NEW_PLAYLIST_ITEM_REQUESTS);
        AtomicInteger successItemsCount = new AtomicInteger(0);

        while (batches.hasNext()) {
            MusicProtocol.ImportMusicPlaylistTracksResponse importMusicPlaylistTracksResponse = importMusicPlaylistItemsBatch(jobId.toString(), batches.next());

            for (MusicProtocol.MusicPlaylistTrackResponse trackResponse : importMusicPlaylistTracksResponse.getMusicPlaylistTrackResponseList()) {
                if (trackResponse.hasStatus() && trackResponse.getStatus().getCode() == SC_OK) {
                    successItemsCount.getAndIncrement();
                    idempotentImportExecutor.executeAndSwallowIOExceptions(
                            trackResponse.getId(),
                            trackResponse.getName(),
                            () -> {
                                monitor.debug(
                                        () -> "Apple importing music playlist track ",
                                        AuditKeys.jobId, jobId,
                                        AuditKeys.dataId, trackResponse.getId());
                                return trackResponse.getId();
                            });
                } else {
                    idempotentImportExecutor.executeAndSwallowIOExceptions(
                            trackResponse.getId(),
                            trackResponse.getName(),
                            () -> {
                                monitor.severe(
                                        () -> "Error importing playlist track: ",
                                        AuditKeys.jobId, jobId,
                                        AuditKeys.dataId, trackResponse.getId());

                                throw new IOException(
                                        String.format(
                                                "Failed to import playlist track, error code: %d",
                                                trackResponse.getStatus().getCode()));
                            });
                }
            }
        }
        return successItemsCount.get();
    }

    public MusicProtocol.ImportMusicPlaylistTracksResponse importMusicPlaylistItemsBatch(final String jobId, List<MusicPlaylistItem> playlistItems)
            throws CopyExceptionWithFailureReason, IOException, URISyntaxException {

        List<MusicProtocol.MusicTrack> convertedMusicPlaylistTracks = AppleMusicPlaylistConverter.convertToAppleMusicPlaylistTracks(playlistItems);
        final MusicProtocol.ImportMusicPlaylistTracksRequest.Builder requestBuilder = MusicProtocol.ImportMusicPlaylistTracksRequest.newBuilder();
        requestBuilder.setImportSessionId(jobId);
        requestBuilder.addAllMusicTrack(convertedMusicPlaylistTracks);

        if (JobMetadata.getExportService() != null) {
            requestBuilder.setExportService(JobMetadata.getExportService());
        }

        final byte[] responseBody = sendPostRequest(baseUrl + API.V1.TRACKS, requestBuilder.build().toByteArray());

        return MusicProtocol.ImportMusicPlaylistTracksResponse.parseFrom(responseBody);
    }

    public byte[] sendPostRequest(@Nonnull final String url, @Nonnull final byte[] requestData)
            throws IOException, URISyntaxException, CopyExceptionWithFailureReason {
        HttpRequest.Builder requestBuilder = createMusicImportRequest(url, requestData);
        return makeMusicServiceRequest(requestBuilder);
    }

    public byte[] makeMusicServiceRequest(@Nonnull final HttpRequest.Builder requestBuilder)
            throws IOException, CopyExceptionWithFailureReason {

        byte[] responseByteArray;
        try {
            responseByteArray = sendRequest(requestBuilder);
        } catch (CopyExceptionWithFailureReason e) {
            if (e instanceof UnconfirmedUserException
                    || e instanceof PermissionDeniedException) {
                refreshTokens();
                responseByteArray = sendRequest(requestBuilder);
            } else {
                throw e;
            }
        }
        return responseByteArray;
    }

    private void refreshTokens() throws InvalidTokenException {
        final String refreshToken = authData.getRefreshToken();
        final String refreshUrlString = authData.getTokenServerEncodedUrl();
        final String clientId = appCredentials.getKey();
        final String clientSecret = appCredentials.getSecret();

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("grant_type", "refresh_token");
        parameters.put("refresh_token", refreshToken);
        StringJoiner sj = new StringJoiner("&");
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            sj.add(entry.getKey() + "=" + entry.getValue());
        }

        try {
            final HttpRequest.Builder refreshRequest = createRefreshTokensRequest(refreshUrlString, sj.toString());
            final String responseString = new String(sendRequest(refreshRequest));
            final JSONParser parser = new JSONParser();
            final JSONObject json = (JSONObject) parser.parse(responseString);
            final String accessToken = (String) json.get("access_token");
            this.authData = new TokensAndUrlAuthData(accessToken, refreshToken, refreshUrlString);

            monitor.debug(() -> "Successfully refreshed token");

        } catch (ParseException | IOException | CopyExceptionWithFailureReason | URISyntaxException e) {
            monitor.debug(() -> "Failed to refresh token", e);
            throw new InvalidTokenException("Unable to refresh token", e);
        }
    }

    private HttpRequest.Builder createRefreshTokensRequest(String refreshUrlString, String body) throws URISyntaxException {
        HttpRequest.Builder requestBuilder = createBaseRequestBuilder(refreshUrlString);
        return requestBuilder
                .POST(HttpRequest.BodyPublishers.ofString(body));
    }

    private HttpRequest.Builder createMusicImportRequest(String url, byte[] body) throws URISyntaxException {
        HttpRequest.Builder requestBuilder = createBaseRequestBuilder(url);
        return requestBuilder
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .header("Content-Type", "application/x-protobuf");
    }

    private HttpRequest.Builder createBaseRequestBuilder(String url) throws URISyntaxException {
        return HttpRequest.newBuilder()
                .uri(new URI(url));
    }

    private byte[] sendRequest(@Nonnull HttpRequest.Builder requestBuilder)
            throws IOException, CopyExceptionWithFailureReason {

        final UUID jobId = JobMetadata.getJobId();
        final String appleRequestUUID = UUID.randomUUID().toString();

        final HttpRequest request;
        HttpResponse<byte[]> response = null;
        try {
            request = requestBuilder.header(Headers.AUTHORIZATION.getValue(), authData.getAccessToken())
                    .header(Headers.CORRELATION_ID.getValue(), appleRequestUUID)
                    .build();

            monitor.info(
                    () -> String.format("%s request from AppleMusicInterface", request.method()),
                    Headers.CORRELATION_ID, appleRequestUUID,
                    AuditKeys.uri, request.uri().toString(),
                    AuditKeys.jobId, jobId.toString()
            );

            HttpClient client = HttpClient.newBuilder().build();
            response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response == null) {
                convertAndThrowException(new RuntimeException("No response from Apple Music Library Service"), SC_INTERNAL_SERVER_ERROR);
            } else {
                if (response.statusCode() > SC_CREATED) {
                    convertAndThrowException(new RuntimeException("Apple Music Library Service Error: " + new String(response.body())), response.statusCode());
                }
            }

        }  catch (IOException | InterruptedException e) {
            monitor.severe(
                    () -> String.format("Exception from %s in AppleMusicInterface", requestBuilder.build().method()),
                    Headers.CORRELATION_ID.getValue(), appleRequestUUID,
                    AuditKeys.jobId, jobId.toString(),
                    AuditKeys.error, e.getMessage(),
                    AuditKeys.errorCode, SC_INTERNAL_SERVER_ERROR,
                    e);

            convertAndThrowException(e, SC_INTERNAL_SERVER_ERROR);
        }

        return response.body();
    }

    private void convertAndThrowException(@Nonnull final Exception e, final int statusCode)
            throws CopyExceptionWithFailureReason, IOException {

        switch (statusCode) {
            case SC_OK:
                break;
            case SC_UNAUTHORIZED:
                throw new UnconfirmedUserException("Unauthorized Apple Music User", e);
            case SC_PRECONDITION_FAILED:
                throw new PermissionDeniedException("Apple Music Library Service Permission Denied", e);
            case SC_NOT_FOUND:
                throw new AppleHttpException("Apple Music Library Not Found", e, SC_NOT_FOUND);
            case SC_INSUFFICIENT_STORAGE:
                throw new DestinationMemoryFullException("Apple Music Library Storage Full", e);
            case SC_SERVICE_UNAVAILABLE:
                throw new IOException("Apple Music Library Service Unavailable", e);
            case SC_BAD_REQUEST:
                throw new IOException("Bad Apple Music Library Service Request", e);
            case SC_INTERNAL_SERVER_ERROR:
            default:
                throw new IOException("Apple Music Library Service Error", e);
        }
    }
}
