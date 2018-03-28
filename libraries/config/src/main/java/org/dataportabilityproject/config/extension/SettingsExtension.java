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
package org.dataportabilityproject.config.extension;


import org.dataportabilityproject.api.launcher.AbstractExtension;
import org.dataportabilityproject.api.launcher.Constants.Environment;

/**
 * Extension that provides settings required by both the API and worker servers.
 *
 * <p>Also provides custom extension settings that extensions use.
 */
public interface SettingsExtension extends AbstractExtension {

  @Flag String cloud();

  @Flag Environment environment();

  /**
   * Returns the configuration value for an extension setting or the default value if not found.
   *
   * @param setting the extension setting name
   * @param defaultValue the default value. Null may be passed.
   */
  <T> T getSetting(String setting, T defaultValue);
}
