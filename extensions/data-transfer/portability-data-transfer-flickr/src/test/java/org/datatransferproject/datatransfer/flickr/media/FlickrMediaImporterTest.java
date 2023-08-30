/*
 * Copyright 2022 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.flickr.media;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.*;

import com.flickr4java.flickr.Flickr;
import com.flickr4java.flickr.FlickrException;
import com.flickr4java.flickr.auth.Auth;
import com.flickr4java.flickr.auth.AuthInterface;
import com.flickr4java.flickr.auth.Permission;
import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photosets.Photoset;
import com.flickr4java.flickr.photosets.PhotosetsInterface;
import com.flickr4java.flickr.uploader.UploadMetaData;
import com.flickr4java.flickr.uploader.Uploader;

import com.github.scribejava.core.model.OAuth1Token;
import java.io.BufferedInputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.cloud.local.LocalJobStore;
import org.datatransferproject.datatransfer.flickr.FlickrTestUtils;
import org.datatransferproject.spi.cloud.storage.TemporaryPerJobDataStore;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class FlickrMediaImporterTest {
    private static final String ALBUM_ID = "Album ID";
    private static final String ALBUM_NAME = "Album name";
    private static final String ALBUM_DESCRIPTION = "Album description";
    private static final String PHOTO_TITLE = "Title";
    private static final String FETCHABLE_URL = "fetchable_url";
    private static final String PHOTO_DESCRIPTION = "Description";
    private static final String MEDIA_TYPE = "jpeg";
    private static final String FLICKR_PHOTO_ID = "flickrPhotoId";
    private static final String FLICKR_VIDEO_ID = "flickrVideoId";
    private static final String FLICKR_ALBUM_ID = "flickrAlbumId";
    private static final String VIDEO_TITLE = "VideoTitle";
    private static final String VIDEO_FETCHABLE_URL = "video_fetchable_url";
    private static final String VIDEO_DESCRIPTION = "VideoDescription";
    private static final String VIDEO_MEDIA_TYPE = "video/mp4";
    private static final String VIDEO_ID = "videoId";

    private static final MediaAlbum Media_ALBUM =
            new MediaAlbum(ALBUM_ID, ALBUM_NAME, ALBUM_DESCRIPTION);
    private static final PhotoModel PHOTO_MODEL =
            new PhotoModel(
                    PHOTO_TITLE, FETCHABLE_URL, PHOTO_DESCRIPTION, MEDIA_TYPE, "MyId", ALBUM_ID, false);
    private static final VideoModel VIDEO_MODEL =
            new VideoModel(
                    VIDEO_TITLE,
                    VIDEO_FETCHABLE_URL,
                    VIDEO_DESCRIPTION,
                    VIDEO_MEDIA_TYPE,
                    VIDEO_ID,
                    ALBUM_ID,
                    false,
                    null);
    private static final UploadMetaData PHOTO_UPLOAD_META_DATA =
            new UploadMetaData()
                    .setAsync(false)
                    .setPublicFlag(false)
                    .setFriendFlag(false)
                    .setFamilyFlag(false)
                    .setTitle(PHOTO_TITLE)
                    .setDescription(PHOTO_DESCRIPTION);
    private static final UploadMetaData VIDEO_UPLOAD_META_DATA =
            new UploadMetaData()
                    .setAsync(false)
                    .setPublicFlag(false)
                    .setFriendFlag(false)
                    .setFamilyFlag(false)
                    .setTitle(VIDEO_TITLE)
                    .setDescription(VIDEO_DESCRIPTION);

    private static final IdempotentImportExecutor EXECUTOR = new FakeIdempotentImportExecutor();

    private Flickr flickr = mock(Flickr.class);
    private PhotosetsInterface photosetsInterface = mock(PhotosetsInterface.class);
    private Uploader uploader = mock(Uploader.class);
    private TemporaryPerJobDataStore jobStore = new LocalJobStore();
    private FlickrMediaImporter.ImageStreamProvider imageStreamProvider =
            mock(FlickrMediaImporter.ImageStreamProvider.class);

    private User user = mock(User.class);
    private Auth auth = new Auth(Permission.WRITE, user);
    private BufferedInputStream bufferedInputStream = mock(BufferedInputStream.class);
    private AuthInterface authInterface = mock(AuthInterface.class);

    private Monitor monitor = mock(Monitor.class);

    @Test
    public void importStoresAlbumInJobStore() throws FlickrException, Exception {
        UUID jobId = UUID.randomUUID();

        MediaContainerResource mediaContainerResource =
                new MediaContainerResource(
                        Collections.singletonList(Media_ALBUM), Collections.singletonList(PHOTO_MODEL), Collections.singletonList(VIDEO_MODEL));

        // Setup Mock
        when(user.getId()).thenReturn("userId");
        when(authInterface.checkToken(any(OAuth1Token.class))).thenReturn(auth);

        when(flickr.getPhotosetsInterface()).thenReturn(photosetsInterface);
        when(flickr.getUploader()).thenReturn(uploader);
        when(flickr.getAuthInterface()).thenReturn(authInterface);
        when(imageStreamProvider.get(FETCHABLE_URL)).thenReturn(bufferedInputStream);
        when(imageStreamProvider.get(VIDEO_FETCHABLE_URL)).thenReturn(bufferedInputStream);
        when(uploader.upload(any(BufferedInputStream.class), refEq(PHOTO_UPLOAD_META_DATA)))
                .thenReturn(FLICKR_PHOTO_ID);
        when(uploader.upload(any(BufferedInputStream.class), refEq(VIDEO_UPLOAD_META_DATA)))
                .thenReturn(FLICKR_VIDEO_ID);

        String flickrAlbumTitle = ALBUM_NAME;
        Photoset photoset =
                FlickrTestUtils.initializePhotoset(FLICKR_ALBUM_ID, ALBUM_DESCRIPTION, FLICKR_PHOTO_ID);
        when(photosetsInterface.create(flickrAlbumTitle, ALBUM_DESCRIPTION, FLICKR_PHOTO_ID))
                .thenReturn(photoset);

        // Run test
        FlickrMediaImporter importer =
                new FlickrMediaImporter(
                        flickr,
                        jobStore,
                        imageStreamProvider,
                        monitor,
                        TransferServiceConfig.getDefaultInstance());
        ImportResult result =
                importer.importItem(
                        jobId, EXECUTOR, new TokenSecretAuthData("token", "secret"), mediaContainerResource);

        // Verify that the image stream provider got the correct URL and that the correct info was
        // uploaded
        verify(imageStreamProvider).get(FETCHABLE_URL);
        ArgumentCaptor<UploadMetaData> uploadMetaDataArgumentCaptor =
                ArgumentCaptor.forClass(UploadMetaData.class);
        verify(uploader, times(2)).upload(eq(bufferedInputStream), uploadMetaDataArgumentCaptor.capture());
        List<UploadMetaData> actualUploadMetaData = uploadMetaDataArgumentCaptor.getAllValues();

        assertThat(actualUploadMetaData.get(0).getTitle()).isEqualTo(PHOTO_TITLE);
        assertThat(actualUploadMetaData.get(0).getDescription()).isEqualTo(PHOTO_DESCRIPTION);
        assertThat(actualUploadMetaData.get(1).getTitle()).isEqualTo(VIDEO_TITLE);
        assertThat(actualUploadMetaData.get(1).getDescription()).isEqualTo(VIDEO_DESCRIPTION);

        // Verify the photosets interface got the command to create the correct album
        verify(photosetsInterface).create(flickrAlbumTitle, ALBUM_DESCRIPTION, FLICKR_PHOTO_ID);

        assertThat((String) EXECUTOR.getCachedValue(ALBUM_ID)).isEqualTo(FLICKR_ALBUM_ID);
    }
}
