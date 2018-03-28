/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.dataportabilityproject.api.launcher.extension;

import org.dataportabilityproject.api.launcher.ApiSettings;

/**
 * Extension that provides {@link ApiSettings}, along with {@code CommonSettings} and extension-
 * specific settings (through inherited {@link SettingsExtension}) to an API server.
 */
public interface ApiSettingsExtension extends SettingsExtension {
  ApiSettings getApiSettings();
}
