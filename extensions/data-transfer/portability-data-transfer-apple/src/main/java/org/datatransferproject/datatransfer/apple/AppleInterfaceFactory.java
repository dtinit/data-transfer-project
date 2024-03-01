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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.google.common.base.Preconditions;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.music.AppleMusicInterface;
import org.datatransferproject.datatransfer.apple.photos.AppleMediaInterface;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * The Factory class that creates the Apple Import Extensions.
 */
public class AppleInterfaceFactory {

  private static final String ERROR_MESSAGE = "%s is required";
  private final Map<UUID, AppleBaseInterface> interfacesByAuthData = new HashMap<>();
  public synchronized AppleMediaInterface getOrCreateMediaInterface(
    UUID jobId,
    TokensAndUrlAuthData authData,
    AppCredentials appCredentials,
    String exportingService,
    Monitor monitor) {
    return (AppleMediaInterface)interfacesByAuthData
      .computeIfAbsent(jobId, key ->  makeMediaInterface(authData, appCredentials,
        exportingService, monitor));
  }

  protected synchronized AppleMediaInterface makeMediaInterface(
          TokensAndUrlAuthData authData,
          AppCredentials appCredentials,
          String exportingService,
          Monitor monitor) {

    Objects.requireNonNull(authData);
    Objects.requireNonNull(appCredentials);
    Objects.requireNonNull(exportingService);
    Objects.requireNonNull(monitor);

    return new AppleMediaInterface(authData, appCredentials, exportingService, monitor);
  }

  public synchronized AppleMusicInterface getOrCreateMusicInterface(
          UUID jobId,
          TokensAndUrlAuthData authData,
          AppCredentials appCredentials,
          String exportingService,
          Monitor monitor) {
    return (AppleMusicInterface) interfacesByAuthData
            .computeIfAbsent(jobId, key -> makeMusicInterface(authData, appCredentials,
                    exportingService, monitor));
  }

  protected synchronized  AppleMusicInterface makeMusicInterface(
          TokensAndUrlAuthData authData,
          AppCredentials appCredentials,
          String exportingService,
          Monitor monitor) {

    Preconditions.checkNotNull(authData, ERROR_MESSAGE, "authData");
    Preconditions.checkNotNull(appCredentials, ERROR_MESSAGE, "appCredentials");
    Preconditions.checkNotNull(exportingService, ERROR_MESSAGE, "exportingService");
    Preconditions.checkNotNull(monitor, ERROR_MESSAGE, "monitor");

    return new AppleMusicInterface(authData, appCredentials, exportingService, monitor);
  }
}