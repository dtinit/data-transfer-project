/*
 * Copyright 2020 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.copier;

import com.google.common.collect.ImmutableList;

import java.util.ServiceLoader;

public class InMemoryDataCopierClassLoader {

  public static Class<? extends InMemoryDataCopier> load() {
    ImmutableList.Builder<InMemoryDataCopierExtension> builder = ImmutableList.builder();
    ServiceLoader.load(InMemoryDataCopierExtension.class).iterator().forEachRemaining(builder::add);
    ImmutableList<InMemoryDataCopierExtension> executors = builder.build();
    if (executors.isEmpty()) {
      return PortabilityInMemoryDataCopier.class;
    } else if (executors.size() == 1) {
      InMemoryDataCopierExtension extension = executors.get(0);
      extension.initialize();
      return extension.getInMemoryDataCopierClass();
    } else {
      throw new IllegalStateException("Cannot load multiple InMemoryDataCopiers");
    }
  }

  private InMemoryDataCopierClassLoader() {}
}
