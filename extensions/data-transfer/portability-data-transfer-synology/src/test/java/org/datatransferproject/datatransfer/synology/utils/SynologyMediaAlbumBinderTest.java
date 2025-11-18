/*
 * Copyright 2025 The Data Transfer Project Authors.
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
 *
 */

package org.datatransferproject.datatransfer.synology.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SynologyMediaAlbumBinderTest {
  @Mock private OnAlbumReadyCallback<String> mockConsumer;
  @Mock private Monitor monitor;
  private final UUID jobId = UUID.randomUUID();
  private final PhotoModel photo =
      new PhotoModel("title1", "url1", "desc", "mediaType", "dataId1", "1", true);
  private final MediaAlbum album = new MediaAlbum("1", "album1", "description1");
  private final String newPhotoId = "10";
  private final String newAlbumId = "100";

  @Test
  public void shouldInvokeReadyHandlerWhenCreateItemBeforeAlbum() {
    SynologyMediaAlbumBinder<String> binder = new SynologyMediaAlbumBinder<>(mockConsumer, monitor);

    binder.put(photo.getAlbumId(), newPhotoId, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());

    binder.whenAlbumReady(album.getId(), newAlbumId, jobId);
    verify(mockConsumer).accept(eq(newAlbumId), eq(newPhotoId), eq(jobId));
  }

  @Test
  public void shouldInvokeReadyHandlerWhenCreateAlbumBeforeItem() {
    SynologyMediaAlbumBinder<String> binder = new SynologyMediaAlbumBinder<>(mockConsumer, monitor);

    binder.whenAlbumReady(album.getId(), newAlbumId, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());

    binder.put(photo.getAlbumId(), newPhotoId, jobId);
    verify(mockConsumer).accept(eq(newAlbumId), eq(newPhotoId), eq(jobId));
  }

  @Test
  public void shouldNotInvokeReadyHandlerWhenCreateItemBeforeAlbumAndKeyNotMatch() {
    SynologyMediaAlbumBinder<String> binder = new SynologyMediaAlbumBinder<>(mockConsumer, monitor);

    binder.put(photo.getAlbumId(), newPhotoId, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());

    binder.whenAlbumReady("2", newAlbumId, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());
  }

  @Test
  public void shouldNotInvokeReadyHandlerWhenCreateAlbumBeforeItemAndKeyNotMatch() {
    SynologyMediaAlbumBinder<String> binder = new SynologyMediaAlbumBinder<>(mockConsumer, monitor);

    binder.whenAlbumReady(album.getId(), newAlbumId, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());

    binder.put("2", newPhotoId, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());
  }

  @Test
  public void shouldNotInvokeReadyHandlerWhenAlbumKeyIsNull() {
    SynologyMediaAlbumBinder<String> binder = new SynologyMediaAlbumBinder<>(mockConsumer, monitor);
    binder.put(null, newPhotoId, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());
  }

  @Test
  public void shouldNotInvokeReadyHandlerWhenNewItemKeyIsNull() {
    SynologyMediaAlbumBinder<String> binder = new SynologyMediaAlbumBinder<>(mockConsumer, monitor);
    binder.put(photo.getAlbumId(), null, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());
  }

  @Test
  public void shouldNotInvokeReadyHandlerWhenNewAlbumKeyIsNull() {
    SynologyMediaAlbumBinder<String> binder = new SynologyMediaAlbumBinder<>(mockConsumer, monitor);
    binder.whenAlbumReady(album.getId(), null, jobId);
    verify(mockConsumer, never()).accept(any(), any(), any());
  }
}
