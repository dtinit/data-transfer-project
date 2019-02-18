/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.spotify.playlists;


import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.UUID;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.playlists.PlaylistContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * Imports playlists into Spotify.
 **/
public class SpotifyPlaylistImporter
    implements Importer<TokensAndUrlAuthData, PlaylistContainerResource> {

  @Override
  public ImportResult importItem(UUID jobId, TokensAndUrlAuthData authData,
      PlaylistContainerResource data) throws Exception {

    return ImportResult.OK;
  }
}
