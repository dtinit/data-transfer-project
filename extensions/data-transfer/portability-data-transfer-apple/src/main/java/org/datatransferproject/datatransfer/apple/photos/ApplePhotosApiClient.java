package org.datatransferproject.datatransfer.apple.photos;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.IOUtils;
import org.datatransferproject.datatransfer.apple.constants.ApplePhotosConstants;
import org.datatransferproject.datatransfer.apple.constants.AuditKeys;
import org.datatransferproject.datatransfer.apple.constants.Headers;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol;
import org.datatransferproject.spi.transfer.types.PermissionDeniedException;
import org.datatransferproject.spi.transfer.types.*;
import org.datatransferproject.transfer.JobMetadata;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.jetbrains.annotations.NotNull;
 import org.datatransferproject.datatransfer.apple.AppleBaseInterface;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol.CreateAlbumsResponse;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol.GetUploadUrlsResponse;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol.CreateMediaResponse;
import org.datatransferproject.datatransfer.apple.photos.photosproto.PhotosProtocol.NewMediaRequest;

import static org.apache.http.HttpStatus.*;

public class ApplePhotosApiClient {
    private final String baseUrl;
    private TokensAndUrlAuthData authData;
    private final AppCredentials appCredentials;
    private final Monitor monitor;

    public ApplePhotosApiClient(
            @NotNull TokensAndUrlAuthData authData,
            @NotNull AppCredentials appCredentials,
            @NotNull Monitor monitor) {
        this.authData = authData;
        this.appCredentials = appCredentials;
        this.monitor = monitor;
        this.baseUrl = "https://datatransfer.apple.com/photos/";
    }

    public CreateAlbumsResponse createAlbums(
            @NotNull final String jobId,
            @NotNull final String dataClass,
            @NotNull final Collection<MediaAlbum> mediaAlbums)
            throws IOException, CopyExceptionWithFailureReason {
        final PhotosProtocol.CreateAlbumsRequest.Builder requestBuilder = PhotosProtocol.CreateAlbumsRequest.newBuilder();
        requestBuilder.setImportSessionId(jobId);
        if (JobMetadata.getExportService() != null) {
            requestBuilder.setExportService(JobMetadata.getExportService());
        }
        requestBuilder.setDataClass(dataClass);
        requestBuilder.addAllNewPhotoAlbumRequests(
                mediaAlbums.stream()
                        .map(mediaAlbum -> PhotosProtocol.NewPhotoAlbumRequest.newBuilder()
                                .setDataId(mediaAlbum.getId())
                                .setName(Optional.ofNullable(mediaAlbum.getName()).orElse(""))
                                .build())
                        .collect(Collectors.toList()));
        final byte[] payload = requestBuilder.build().toByteArray();
        final byte[] responseData = makePhotosServicePostRequest(baseUrl + "createalbums", payload);
        return CreateAlbumsResponse.parseFrom(responseData);
    }

    public GetUploadUrlsResponse getUploadUrl(
            @NotNull final String jobId,
            @NotNull final String dataClass,
            @NotNull final List<String> dataIds)
            throws IOException, CopyExceptionWithFailureReason {
        final List<PhotosProtocol.AuthorizeUploadRequest> uploadRequests = dataIds.stream()
                .map(dataId -> PhotosProtocol.AuthorizeUploadRequest.newBuilder().setDataId(dataId).build())
                .collect(Collectors.toList());
        final PhotosProtocol.GetUploadUrlsRequest.Builder requestBuilder = PhotosProtocol.GetUploadUrlsRequest.newBuilder();
        if (JobMetadata.getExportService() != null) {
            requestBuilder.setExportService(JobMetadata.getExportService());
        }
        requestBuilder.setDataClass(dataClass)
                .setImportSessionId(jobId)
                .addAllUploadRequests(uploadRequests);
        final byte[] payload = requestBuilder.build().toByteArray();
        final byte[] responseData = makePhotosServicePostRequest(baseUrl + "getuploadurls", payload);
        return GetUploadUrlsResponse.parseFrom(responseData);
    }

    public CreateMediaResponse createMedia(
            @NotNull final String jobId,
            @NotNull final String dataClass,
            @NotNull final List<NewMediaRequest> newMediaRequestList)
            throws IOException, CopyExceptionWithFailureReason {
        final PhotosProtocol.CreateMediaRequest.Builder requestBuilder = PhotosProtocol.CreateMediaRequest.newBuilder();
        requestBuilder.setImportSessionId(jobId)
                .setDataClass(dataClass)
                .addAllNewMediaRequests(newMediaRequestList);
        final byte[] payload = requestBuilder.build().toByteArray();
        final byte[] responseData = makePhotosServicePostRequest(baseUrl + "createmedia", payload);
        return CreateMediaResponse.parseFrom(responseData);
    }

    public byte[] makePhotosServicePostRequest(
            @NotNull final String url,
            @NotNull final byte[] requestData)
            throws IOException, CopyExceptionWithFailureReason {
        byte[] responseData = null;
        try {
            final String responseString = sendPostRequest(url, requestData);
            responseData = responseString.getBytes(StandardCharsets.ISO_8859_1);
        } catch (CopyExceptionWithFailureReason e) {
            if (e instanceof UnconfirmedUserException || e instanceof PermissionDeniedException) {
                this.authData = AppleBaseInterface.refreshTokens(authData, appCredentials, monitor);
                final String responseString = sendPostRequest(url, requestData);
                responseData = responseString.getBytes(StandardCharsets.ISO_8859_1);
            } else {
                throw e;
            }
        }
        return responseData;
    }



    private String sendPostRequest(@NotNull String url, @NotNull final byte[] requestData)
            throws IOException, CopyExceptionWithFailureReason {
        final String appleRequestUUID = UUID.randomUUID().toString();
        final UUID jobId = JobMetadata.getJobId();
        monitor.info(
                () -> "POST Request from ApplePhotosApiClient",
                Headers.CORRELATION_ID, appleRequestUUID,
                AuditKeys.uri, url,
                AuditKeys.jobId, jobId.toString());

        HttpURLConnection con = null;
        String responseString = "";
        try {
            URL applePhotosUrl = new URL(url);
            con = (HttpURLConnection) applePhotosUrl.openConnection();
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty(Headers.AUTHORIZATION.getValue(), authData.getAccessToken());
            con.setRequestProperty(Headers.CORRELATION_ID.getValue(), appleRequestUUID);
            if (url.contains(baseUrl)) {
                con.setRequestProperty(Headers.CONTENT_TYPE.getValue(), "");
            }
            IOUtils.write(requestData, con.getOutputStream());
            responseString = IOUtils.toString(con.getInputStream(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            monitor.severe(
                    () -> "Exception from POST in ApplePhotosApiClient",
                    Headers.CORRELATION_ID.getValue(), appleRequestUUID,
                    AuditKeys.jobId, jobId.toString(),
                    AuditKeys.error, e.getMessage(),
                    AuditKeys.errorCode, con.getResponseCode(),
                    e);
            convertAndThrowException(e, con);
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return responseString;
    }
    public static String getApplePhotosImportThrowingMessage(
            final String cause,
            final ImmutableMap<AuditKeys, Optional<String>> keyValuePairs) {
        String finalLogMessage = String.format("%s " + cause, ApplePhotosConstants.APPLE_PHOTOS_IMPORT_ERROR_PREFIX);
        for (AuditKeys key: keyValuePairs.keySet()){
            finalLogMessage = String.format("%s, %s:%s", finalLogMessage, key.name(), keyValuePairs.get(key).orElse("[n/a]"));
        }
        return finalLogMessage;
    }
    public static String getApplePhotosImportThrowingMessage(final String cause) {
        return getApplePhotosImportThrowingMessage(cause, ImmutableMap.of());
    }
    private void convertAndThrowException(@NotNull final IOException e, @NotNull final HttpURLConnection con)
            throws IOException, CopyExceptionWithFailureReason {
        switch (con.getResponseCode()) {
            case SC_UNAUTHORIZED:
                throw new UnconfirmedUserException(getApplePhotosImportThrowingMessage("Unauthorized iCloud User"), e);
            case SC_PRECONDITION_FAILED:
                throw new PermissionDeniedException(getApplePhotosImportThrowingMessage("Permission Denied"), e);
            case SC_NOT_FOUND:
                throw new DestinationNotFoundException(getApplePhotosImportThrowingMessage("iCloud Photos Library not found"), e);
            case SC_INSUFFICIENT_STORAGE:
                throw new DestinationMemoryFullException(getApplePhotosImportThrowingMessage("iCloud Storage is full"), e);
            case SC_SERVICE_UNAVAILABLE:
                throw new IOException(getApplePhotosImportThrowingMessage("DTP import service unavailable"), e);
            case SC_BAD_REQUEST:
                throw new IOException(getApplePhotosImportThrowingMessage("Bad request sent to iCloud Photos import api"), e);
            case SC_INTERNAL_SERVER_ERROR:
                throw new IOException(getApplePhotosImportThrowingMessage("Internal server error in iCloud Photos service"), e);
            case SC_OK:
                break;
            default:
                throw e;
        }
    }
}