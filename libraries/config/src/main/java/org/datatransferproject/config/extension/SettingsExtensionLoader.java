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
package org.datatransferproject.config.extension;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ServiceLoader;

/** Helper for loading the settings extension in a runtime. */
public class SettingsExtensionLoader {

  public static SettingsExtension getSettingsExtension() {
    ImmutableList.Builder<SettingsExtension> extensionsBuilder = ImmutableList.builder();
    ServiceLoader.load(SettingsExtension.class).iterator().forEachRemaining(extensionsBuilder::add);
    ImmutableList<SettingsExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        extensions.size() == 1,
        "Exactly one SettingsExtension is required, but found " + extensions.size());
    return extensions.get(0);
  }

  private SettingsExtensionLoader() {}
}
