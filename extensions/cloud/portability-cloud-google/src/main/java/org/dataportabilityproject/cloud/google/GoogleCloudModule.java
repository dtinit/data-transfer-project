/*
 * Copyright 2018 The Data-Portability Project Authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.cloud.google;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.dataportabilityproject.api.launcher.ExtensionContext;

/** Bindings for cloud platform components using Google Cloud Platform. **/
final class GoogleCloudModule extends AbstractModule {
  @BindingAnnotation
  @Target({ FIELD, PARAMETER, METHOD }) @Retention(RUNTIME)
  public @interface ProjectId {}

  @Override
  protected void configure() {}

  @Provides
  @Inject
  GoogleCredentials getCredentials(ExtensionContext context, @ProjectId String projectId)
      throws GoogleCredentialException {
    validateUsingGoogle(context);

    String env = context.getConfiguration("environment", "LOCAL");
    if (env.equals("LOCAL")) { // Running locally
      // This is a crude check to make sure we are only pointing to test projects when running
      // locally and connecting to GCP
      Preconditions.checkArgument(
          projectId.endsWith("-local") || projectId.endsWith("-test") || projectId.endsWith("-qa"),
          "Invalid project to connect to with env=LOCAL. " + projectId + " doesn't appear to"
              + " be a local/test project since it doesn't end in -local, -test, or -qa.");
    } else { // Assume running on GCP
      // TODO: Check whether we are actually running on GCP once we find out how
      String credsLocation = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
      if (!credsLocation.startsWith("/var/secrets/")) {
        String cause = String.format("You are attempting to obtain credentials from somewhere "
            + "other than Kubernetes secrets in prod. You may have accidentally copied creds "
            + "into your image, which we provide as a local debugging mechanism only. See GCP "
            + "build script (config/gcp/build_and_upload_docker_image.sh) for more info. Creds "
            + "location was: %s", credsLocation);
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
  @Provides @Singleton
  @ProjectId
  String getProjectId(ExtensionContext context) {
    validateUsingGoogle(context);
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
    return projectId;
  }

  /**
   * Validate we are using Google Cloud. Should be called in all Providers in this module.
   *
   * <p>This allows us to install this module and rely on Guice-ified flag parsing, and not do a
   * separate parse to conditionally install this module.
   *
   * TODO: Can this be removed? In the new modular structure, will this code only be run/loaded for
   * Google cloud?
   */
  private void validateUsingGoogle(ExtensionContext context) {
    if (!context.getConfiguration("cloud", "").equals("GOOGLE")) {
      throw new IllegalStateException("Injecting Google objects when cloud != Google!");
    }
  }
}
