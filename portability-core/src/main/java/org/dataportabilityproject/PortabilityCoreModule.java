/*
* Copyright 2017 The Data-Portability Project Authors.
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
package org.dataportabilityproject;

import static org.dataportabilityproject.job.JWTTokenManager.JWT_KEY_NAME;
import static org.dataportabilityproject.job.JWTTokenManager.JWT_SECRET_NAME;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.cloud.google.GoogleCloudFactory;
import org.dataportabilityproject.cloud.google.GoogleCloudModule;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.job.JWTTokenManager;
import org.dataportabilityproject.job.TokenManager;
import org.dataportabilityproject.serviceProviders.flickr.FlickrModule;
import org.dataportabilityproject.serviceProviders.google.GoogleModule;
import org.dataportabilityproject.serviceProviders.instagram.InstagramModule;
import org.dataportabilityproject.serviceProviders.microsoft.MicrosoftModule;
import org.dataportabilityproject.serviceProviders.rememberTheMilk.RememberTheMilkModule;
import org.dataportabilityproject.serviceProviders.smugmug.SmugmugModule;
import org.dataportabilityproject.shared.AppCredentialFactory;
import org.dataportabilityproject.shared.CloudAppCredentialFactory;
import org.dataportabilityproject.shared.local.LocalAppCredentialFactory;
import org.dataportabilityproject.shared.settings.CommonSettings;

public final class PortabilityCoreModule extends AbstractModule {
  @Override
  protected void configure() {
    // TODO: selectively load these
    install(new FlickrModule());
    install(new GoogleModule());
    install(new InstagramModule());
    install(new MicrosoftModule());
    install(new RememberTheMilkModule());
    install(new SmugmugModule());

    install(new GoogleCloudModule());
  }

  @Singleton
  @Provides
  CommonSettings provideCommonSettings() {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      InputStream in =
          CommonSettings.class.getClassLoader().getResourceAsStream("settings/common.yaml");
      CommonSettings commonSettings = mapper.readValue(in, CommonSettings.class);
      return commonSettings;
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing common settings", e);
    }
  }

  // TODO: The following is a hack, we need to refactor this when we switch over to JPMS.

  @Provides
  CloudFactory provideCloudFactory(
      CommonSettings commonSettings,
      Provider<LocalCloudFactory> localCloudFactoryProvider,
      Provider<GoogleCloudFactory> googleCloudFactoryProvider) {
    if (commonSettings.getCloud() == SupportedCloud.LOCAL) {
      return localCloudFactoryProvider.get();
    } else if (commonSettings.getCloud() == SupportedCloud.GOOGLE) {
      return googleCloudFactoryProvider.get();
    } else {
      throw new UnsupportedOperationException(commonSettings.getCloud() + " is not supported yet.");
    }
  }

  @Provides
  AppCredentialFactory provideAppCredentialFactory(
      CommonSettings commonSettings,
      Provider<LocalAppCredentialFactory> localAppCredentialFactoryProvider,
      Provider<CloudAppCredentialFactory> cloudAppCredentialFactoryProvider) {
    if (commonSettings.getCloud() == SupportedCloud.LOCAL) {
      return localAppCredentialFactoryProvider.get();
    } else {
      return cloudAppCredentialFactoryProvider.get();
    }
  }

  @Singleton
  @Provides
  TokenManager provideTokenManager(AppCredentialFactory appCredentialFactory) {
    try {
      return new JWTTokenManager(
          appCredentialFactory.lookupAndCreate(JWT_KEY_NAME, JWT_SECRET_NAME).secret());
    } catch (IOException e) {
      throw new IllegalArgumentException(e);
    }
  }
}
