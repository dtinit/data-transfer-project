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

package org.dataportabilityproject.config;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

/**
 * Provides {@link CommonSettings}.
 *
 * <p>Lightweight class that just exposes the static CommonSettings singleton for core components
 * to inject. Extensions, on the other hand, do not necessarily use Guice, so those may retrieve
 * {@link CommonSettings} via the static {@link CommonSettings#getCommonSettings()} method directly.
 */
public class CommonSettingsModule extends AbstractModule {
  @Override
  protected void configure() {}

  @Provides
  CommonSettings getStaticCommonSettings() {
    return CommonSettings.getCommonSettings();
  }
}
