
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

package org.datatransferproject.datatransfer.imgur.photos;

import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.photos.PhotosContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.UUID;

public class ImgurPhotosImporter implements Importer<TokensAndUrlAuthData, PhotosContainerResource> {

    private AppCredentials appCredentials;

    public ImgurPhotosImporter(AppCredentials appCredentials) {
        this.appCredentials = appCredentials;
    }

    @Override
    public ImportResult importItem(UUID jobId, TokensAndUrlAuthData authData, PhotosContainerResource data) throws Exception {
        return null;
    }
}
