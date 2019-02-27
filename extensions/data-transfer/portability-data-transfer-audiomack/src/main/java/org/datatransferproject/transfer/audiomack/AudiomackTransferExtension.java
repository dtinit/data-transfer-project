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

package org.datatransferproject.transfer.audiomack;

import com.google.api.client.http.HttpTransport;
import com.google.common.collect.ImmutableList;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.playlists.PlaylistContainerResource;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

public class AudiomackTransferExtension implements TransferExtension {
  private static final ImmutableList<String> SUPPORTED_DATA_TYPES = ImmutableList.of("PLAYLISTS");

  private Exporter<TokensAndUrlAuthData, PlaylistContainerResource> exporter;
  private Importer<TokensAndUrlAuthData, PlaylistContainerResource> importer;

  private boolean initialized = false;

  @Override
  public String getServiceId() {
    return "Audiomack";
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    return null;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    return null;
  }

  @Override
  public void initialize(ExtensionContext context) {
    if (initialized) {
      Monitor monitor = context.getMonitor();
      monitor.severe(() -> "AudiomackTransferExtension already initialized");
      return;
    }

    Monitor monitor = context.getMonitor();
    HttpTransport httpTransport = context.getService(HttpTransport.class);
  }
}
