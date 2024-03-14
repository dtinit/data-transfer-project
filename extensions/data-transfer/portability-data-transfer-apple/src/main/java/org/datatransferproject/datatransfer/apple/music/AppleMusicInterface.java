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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterators;
import com.google.common.collect.UnmodifiableIterator;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.AppleBaseInterface;
import org.datatransferproject.datatransfer.apple.constants.AppleMusicConstants;
import org.datatransferproject.datatransfer.apple.constants.AuditKeys;
import org.datatransferproject.datatransfer.apple.constants.Headers;
import org.datatransferproject.datatransfer.apple.exceptions.AppleHttpCopyException;
import org.datatransferproject.datatransfer.apple.music.data.converters.AppleMusicPlaylistConverter;
import org.datatransferproject.datatransfer.apple.music.musicproto.MusicProtocol;
import org.datatransferproject.datatransfer.apple.music.musicproto.MusicProtocol.ImportMusicPlaylistsRequest;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.spi.transfer.types.DestinationMemoryFullException;
import org.datatransferproject.spi.transfer.types.InvalidTokenException;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.UnconfirmedUserException;
import org.datatransferproject.spi.transfer.types.UpstreamApiUnexpectedResponseException;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.apache.http.HttpStatus.SC_CREATED;
import static org.apache.http.HttpStatus.SC_INSUFFICIENT_STORAGE;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_PRECONDITION_FAILED;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.datatransferproject.datatransfer.apple.http.TokenRefresher.buildRefreshRequestUrlForAccessToken;

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


    /**
     * @return the total number of playlists acknowledged by the Apple Music Library Service in this page
     */
    public int importPlaylists(final UUID jobId,
            IdempotentImportExecutor idempotentImportExecutor,
            Collection<MusicPlaylist> musicPlaylists) throws Exception {

        UnmodifiableIterator<List<MusicPlaylist>> batches = Iterators.partition(musicPlaylists.iterator(), AppleMusicConstants.MAX_NEW_PLAYLIST_REQUESTS);
        AtomicInteger successPlaylistsCount = new AtomicInteger(0);


        while (batches.hasNext()) {
            MusicProtocol.ImportMusicPlaylistsResponse importMusicPlaylistsResponse = importPlaylistsBatch(jobId.toString(), batches.next());

            for (MusicProtocol.MusicPlaylistResponse playlistResponse : importMusicPlaylistsResponse.getMusicPlaylistResponseList()) {
                String playlistId = playlistResponse.getId();
                String playlistName = playlistResponse.getName();
                if (playlistResponse.hasStatus() && playlistResponse.getStatus().getCode() == SC_OK) {
                    successPlaylistsCount.getAndIncrement();
                    idempotentImportExecutor.executeAndSwallowIOExceptions(
                            playlistId,
                            playlistName,
                            () -> {
                                monitor.debug(
                                        () -> "Apple importing music playlist",
                                        AuditKeys.jobId, jobId,
                                        AuditKeys.playlistId, playlistResponse.getId());
                                return playlistResponse.getId();
                            });
                } else {
                    idempotentImportExecutor.executeAndSwallowIOExceptions(
                            playlistId,
                            playlistName,
                            () -> {
                                monitor.severe(
                                        () -> "Error importing playlist to Apple Music: ",
                                        AuditKeys.jobId, jobId,
                                        AuditKeys.playlistId, playlistResponse.getId());

                                throw new IOException(
                                        String.format(
                                                "Failed to create playlist in Apple Music, server's error code: %d, error message: %s",
                                                playlistResponse.getStatus().getCode(),
                                                playlistResponse.getStatus().getMessage()));
                            });
                }
            }
        }
        return successPlaylistsCount.get();
    }

    @VisibleForTesting
    public MusicProtocol.ImportMusicPlaylistsResponse importPlaylistsBatch(final String jobId, List<MusicPlaylist> musicPlaylistsBatch)
            throws IOException, CopyException, URISyntaxException {
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

    /**
     * @return the total number of playlist tracks acknowledged by the Apple Music Library Service in this page
     */
    public int importMusicPlaylistItems(
            final UUID jobId,
            IdempotentImportExecutor idempotentImportExecutor,
            List<MusicPlaylistItem> musicPlaylistItems) throws Exception {

        UnmodifiableIterator<List<MusicPlaylistItem>> batches = Iterators.partition(musicPlaylistItems.iterator(), AppleMusicConstants.MAX_NEW_PLAYLIST_ITEM_REQUESTS);
        AtomicInteger successItemsCount = new AtomicInteger(0);

        while (batches.hasNext()) {
            MusicProtocol.ImportMusicPlaylistTracksResponse importMusicPlaylistTracksResponse = importMusicPlaylistItemsBatch(jobId.toString(), batches.next());

            for (MusicProtocol.MusicPlaylistTrackResponse trackResponse : importMusicPlaylistTracksResponse.getMusicPlaylistTrackResponseList()) {
                String trackId = trackResponse.getId();
                String trackName = trackResponse.getName();
                if (trackResponse.hasStatus() && trackResponse.getStatus().getCode() == SC_OK) {
                    successItemsCount.getAndIncrement();
                    idempotentImportExecutor.executeAndSwallowIOExceptions(
                            trackId,
                            trackName,
                            () -> {
                                monitor.debug(
                                        () -> "Apple importing music playlist track ",
                                        AuditKeys.jobId, jobId,
                                        AuditKeys.dataId, trackResponse.getId());
                                return trackResponse.getId();
                            });
                } else {
                    idempotentImportExecutor.executeAndSwallowIOExceptions(
                            trackId,
                            trackName,
                            () -> {
                                monitor.severe(
                                        () -> "Error importing playlist track: ",
                                        AuditKeys.jobId, jobId,
                                        AuditKeys.dataId, trackResponse.getId());

                                throw new IOException(
                                        String.format(
                                                "Failed to import playlist track, error code: %d, error message: %s",
                                                trackResponse.getStatus().getCode(),
                                                trackResponse.getStatus().getMessage()));
                            });
                }
            }
        }
        return successItemsCount.get();
    }

    @VisibleForTesting
    public MusicProtocol.ImportMusicPlaylistTracksResponse importMusicPlaylistItemsBatch(final String jobId, List<MusicPlaylistItem> playlistItems)
            throws CopyException, IOException, URISyntaxException {

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

    private byte[] sendPostRequest(@Nonnull final String url, @Nonnull final byte[] requestData)
            throws URISyntaxException, CopyException {
        HttpRequest.Builder requestBuilder = createMusicImportRequest(new URI(url), requestData);
        return makeMusicServiceRequest(requestBuilder);
    }

    private byte[] makeMusicServiceRequest(@Nonnull final HttpRequest.Builder requestBuilder)
            throws CopyException {

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

    private void refreshTokens() throws CopyException {
        final HttpRequest.Builder refreshRequest = buildRefreshRequestUrlForAccessToken(
            authData, appCredentials);
        final String responseString;
        try {
            responseString = new String(sendRequest(refreshRequest));
        } catch (CopyException e) {
            // TODO(jzacsh, hgandhi90) CopyException should never happen; consider refactoring
            // convertAndThrowException from the internals of low-level http logic like sendRequest
            // and sendPost, so callers can interpret such errors instead.
            // - eg: sendRequest -> sendHttpMETHODRequest throws IOException, InterruptedException
            // - eg: sendAppleCopyRequest -> has convertAndThrowException logic
            monitor.debug(() -> "Failed to refresh Apple Music token", e);
            throw new InvalidTokenException("Unable to refresh Apple Music token", e);
        }
        final JSONParser jsonParser = new JSONParser();
        final String refreshedAccessToken;
        try {
            final JSONObject json = (JSONObject) jsonParser.parse(responseString);
            refreshedAccessToken = checkNotNull(
                (String) json.get("access_token"),
                "apple oauth server response body missing access_token, despite OK response");
        } catch (IllegalStateException | ParseException e) {
          throw new UpstreamApiUnexpectedResponseException(String.format(
                  "apple oauth server sent back malformed refresh token response for body:\n\"\"\"\n%s\"\"\"\n\"\"\"", responseString), e);
        }
        this.authData = this.authData.rebuildWithRefresh(refreshedAccessToken);

        monitor.debug(() -> "Successfully refreshed Apple Music token");
    }

    private HttpRequest.Builder createMusicImportRequest(URI url, byte[] body) {
        HttpRequest.Builder requestBuilder = createBaseRequestBuilder(url);
        return requestBuilder
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .header("Content-Type", "application/x-protobuf");
    }

    private HttpRequest.Builder createBaseRequestBuilder(URI url) {
        return HttpRequest.newBuilder()
                .uri(url);
    }

    private byte[] sendRequest(@Nonnull HttpRequest.Builder requestBuilder)
            throws CopyException {

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
            throws CopyException {

        switch (statusCode) {
            case SC_OK:
                break;
            case SC_UNAUTHORIZED:
                throw new UnconfirmedUserException(String.format("http status code: %d: Unauthorized Apple Music User", statusCode), e);
            case SC_PRECONDITION_FAILED:
                throw new PermissionDeniedException(String.format("http status code: %d: Apple Music Library Service Permission Denied", statusCode), e);
            case SC_NOT_FOUND:
                throw new AppleHttpCopyException(String.format("http status code: %d: Apple Music Library Not Found", statusCode), e, SC_NOT_FOUND);
            case SC_INSUFFICIENT_STORAGE:
                throw new DestinationMemoryFullException(String.format("http status code: %d: Apple Music Library Storage Full", statusCode), e);
            case SC_SERVICE_UNAVAILABLE:
                throw new AppleHttpCopyException(String.format("http status code: %d: Apple Music Library Service Unavailable", statusCode), e, SC_SERVICE_UNAVAILABLE);
            case SC_BAD_REQUEST:
                throw new AppleHttpCopyException(String.format("http status code: %d: Bad Apple Music Library Service Request", statusCode), e, SC_BAD_REQUEST);
            case SC_INTERNAL_SERVER_ERROR:
            default:
                throw new AppleHttpCopyException(String.format("http status code: %d: Unknown Apple Music Library Service Error", statusCode), e, SC_INTERNAL_SERVER_ERROR);
        }
    }
}
