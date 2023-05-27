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

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.photos.AppleMediaInterface;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/**
 * The Factory class that creates the Apple Import Extensions.
 */
public class AppleInterfaceFactory {

    private static String errorMessage = "%s is required";

    private Map<UUID, AppleBaseInterface> interfacesByAuthData = new HashMap<>();

    public synchronized AppleMediaInterface getOrCreateMediaInterface(UUID jobId, TokensAndUrlAuthData authData, AppCredentials appCredentials, String exportingService, Monitor monitor) {
        AppleMediaInterface mediaInterface = (AppleMediaInterface) interfacesByAuthData.computeIfAbsent(jobId, key -> makeMediaInterface(authData, appCredentials, exportingService, monitor));
        return mediaInterface;
    }

    protected synchronized AppleMediaInterface makeMediaInterface(TokensAndUrlAuthData authData, AppCredentials appCredentials, String exportingService, Monitor monitor) {
        Preconditions.checkNotNull(authData, errorMessage, "authData");
        Preconditions.checkNotNull(appCredentials, errorMessage, "appCredentials");
        Preconditions.checkNotNull(exportingService, errorMessage, "exportingService");
        Preconditions.checkNotNull(monitor, errorMessage, "monitor");
        return new AppleMediaInterface(authData, appCredentials, exportingService, monitor);
    }
}
