package org.dataportabilityproject.cloud;

import java.util.HashMap;
import java.util.Map;
import org.dataportabilityproject.cloud.google.GoogleCloudFactory;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.shared.Secrets;

public final class CloudFactoryFactory {
  private static Map<SupportedCloud, CloudFactory> cachedFactories = new HashMap<>();

  public static synchronized  CloudFactory getCloudFactory(SupportedCloud cloud, Secrets secrets) {
    if (cachedFactories.containsKey(cloud)) {
      return cachedFactories.get(cloud);
    }

    CloudFactory factory = create(cloud, secrets);
    cachedFactories.put(cloud, factory);
    return factory;
  }

  private static CloudFactory create(SupportedCloud cloud, Secrets secrets) {
    switch (cloud) {
      case LOCAL:
        return new LocalCloudFactory();
      case GOOGLE:
        return new GoogleCloudFactory(secrets);
      default:
        throw new IllegalArgumentException("Don't know how to create cloud: " + cloud);
    }
  }
}
