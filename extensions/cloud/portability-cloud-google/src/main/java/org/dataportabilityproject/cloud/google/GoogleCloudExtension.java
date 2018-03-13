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
package org.dataportabilityproject.cloud.google;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.common.base.Preconditions;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.cloud.google.GoogleCloudExtensionModule.Environment;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.BucketStore;
import org.dataportabilityproject.spi.cloud.storage.CryptoKeyStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;

/**
 * {@link CloudExtension} for Google Cloud Platform.
 */
public class GoogleCloudExtension implements CloudExtension {
  private Injector injector;
  private boolean initialized = false;

  // TODO(seehamrun): Needed for ServiceLoader?
  public GoogleCloudExtension() {}

  /*
   * Initializes the GoogleCloudExtension based on the ExtensionContext.
   */
  @Override
  public void initialize(ExtensionContext context) {
    Preconditions.checkArgument(
        !initialized, "Attempting to initialize GoogleCloudExtension more than once");
    HttpTransport httpTransport = context.getService(HttpTransport.class);
    JsonFactory jsonFactory = context.getService(JsonFactory.class);
    // TODO: "cloud" and "environment" should be global flag names
    String cloud = context.getConfiguration("cloud", "");
    Environment environment =
        Environment.valueOf(context.getConfiguration("environment", Environment.LOCAL.name()));
    injector = Guice.createInjector(new GoogleCloudExtensionModule(httpTransport, jsonFactory,
        cloud, environment));
    initialized = true;
  }

  @Override
  public void shutdown() {
    this.initialized = false;
  }

  @Override
  public JobStore getJobStore() {
    return injector.getInstance(JobStore.class);
  }

  @Override
  public BucketStore getBucketStore() {
    return injector.getInstance(BucketStore.class);
  }

  @Override
  public CryptoKeyStore getCryptoKeyStore() {
    return injector.getInstance(CryptoKeyStore.class);
  }
}
