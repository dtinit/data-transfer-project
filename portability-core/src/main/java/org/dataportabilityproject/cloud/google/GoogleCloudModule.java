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
package org.dataportabilityproject.cloud.google;

import static org.dataportabilityproject.shared.Config.Environment.LOCAL;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import org.dataportabilityproject.cloud.google.Annotations.ProjectId;
import org.dataportabilityproject.shared.settings.CommonSettings;

public class GoogleCloudModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(String.class).annotatedWith(ProjectId.class).toInstance(getProjectId());
  }

  @Provides
  @Inject
  GoogleCredentials getCredentials(CommonSettings commonSettings) throws GoogleCredentialException {
    // TODO: Check whether we are actually running on GCP once we find out how
    boolean isRunningOnGcp = commonSettings.getEnv() != LOCAL;
    // DO NOT REMOVE this security check! This ensures we are using the correct GCP credentials.
    if (isRunningOnGcp) {
      String credsLocation = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
      if (!credsLocation.startsWith("/var/secrets/")) {
        String cause = String.format("You are attempting to obtain credentials from somewhere "
                + "other than Kubernetes secrets in prod. You may have accidentally copied creds into"
                + "your image, which we provide as a local debugging mechanism only. See GCP build "
                + "script (config/gcp/build_and_upload_docker_image.sh) for more info. Creds location was: %s",
            credsLocation);
        throw new GoogleCredentialException(cause);
      }
      // Note: Tried an extra check via Kubernetes API to verify GOOGLE_APPLICATION_CREDENTIALS
      // is the same as the secret via Kubernetes, but this API did not seem reliable.
      // (io.kubernetes.client.apis.CoreV1Api.listSecretForAllNamespaces)
    }
    try {
      return GoogleCredentials.getApplicationDefault();
    } catch (IOException e) {
      throw new GoogleCredentialException(
          "Problem obtaining credentials via GoogleCredentials.getApplicationDefault()", e);
    }
  }

  /**
   * Get project ID from environment variable and validate it is set.
   *
   * @throws IllegalArgumentException if project ID is unset
   */

  private static String getProjectId() {
    String projectId;
    try {
      projectId = System.getenv("GOOGLE_PROJECT_ID");
    } catch (NullPointerException e) {
      throw new IllegalArgumentException("Need to specify a project ID when using Google Cloud. "
          + "This should be exposed as an environment variable by Kubernetes, see "
          + "k8s/api-deployment.yaml");
    }
    Preconditions.checkArgument(!Strings.isNullOrEmpty(projectId), "Need to specify a project "
        + "ID when using Google Cloud. This should be exposed as an environment variable by "
        + "Kubernetes, see k8s/api-deployment.yaml");
    return projectId.toLowerCase();
  }
}
