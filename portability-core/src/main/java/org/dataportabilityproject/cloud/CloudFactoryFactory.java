/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.cloud;

import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.cloud.google.GoogleCloudFactory;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;

public final class CloudFactoryFactory {
  private static Map<SupportedCloud, CloudFactory> cachedFactories = new HashMap<>();

  public static synchronized CloudFactory getCloudFactory(SupportedCloud cloud) {
    if (cachedFactories.containsKey(cloud)) {
      return cachedFactories.get(cloud);
    }

    CloudFactory factory = create(cloud);
    cachedFactories.put(cloud, factory);
    return factory;
  }

  private static CloudFactory create(SupportedCloud cloud) {
    switch (cloud) {
      case LOCAL:
        return new LocalCloudFactory();
      case GOOGLE:
        return new GoogleCloudFactory();
      default:
        throw new IllegalArgumentException("Don't know how to create cloud: " + cloud);
    }
  }
}
