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

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.dataportabilityproject.api.launcher.ExtensionContext;

/** Bindings for cloud platform components using Google Cloud Platform. * */
final class GoogleCloudModule extends AbstractModule {
  // The value for the 'cloud' flag when hosting on Google Cloud Platform.
  private static final String GOOGLE_CLOUD_NAME = "GOOGLE";
  // Environment variable where GCP project ID is stored. The value is set in
  // config/k8s/api-deployment.yaml.
  private static final String GCP_PROJECT_ID_ENV_VAR = "GOOGLE_PROJECT_ID";
  // Environment variable for path where GCP credentials are stored. The value of the environment
  // variable (i.e. the path to creds) is configured in config/k8s/api-deployment.yaml. The creds
  // themselves are exposed as a Kubernetes secret.
  private static final String GCP_CREDENTIALS_PATH_ENV_VAR = "GOOGLE_APPLICATION_CREDENTIALS";
  // The path where Kubernetes stores Secrets.
  private static final String KUBERNETES_SECRETS_PATH_ROOT = "/var/secrets/";

  /**
   * Return the {@link Environment} we are running in based on our convention of Project ID's ending
   * in "-" followed by lower-case environment name.
   *
   * <p>Throws {@link IllegalArgumentException} if projectId does not have a valid environment
   * suffix.
   */
  @VisibleForTesting
  static Environment getProjectEnvironment(String projectId) {
    String[] projectIdComponents = projectId.split("-");
    Preconditions.checkArgument(
        projectIdComponents.length > 1,
        "Invalid project ID - does not end"
            + " in '-' followed by a lower-case environment, e.g. acme-qa");
    String endComponent = projectIdComponents[projectIdComponents.length - 1];
    return Environment.valueOf(endComponent.toUpperCase());
  }

  @Override
  protected void configure() {
    requireBinding(ExtensionContext.class);
  }

  @Provides
  GoogleCredentials getCredentials(ExtensionContext context, @ProjectId String projectId)
      throws GoogleCredentialException {
    validateUsingGoogle(context);

    // TODO(rtannenbaum): Make env a direct parameter, maybe using @Named annotations
    Environment env =
        Environment.valueOf(context.getConfiguration("environment", Environment.LOCAL.name()));
    if (env == Environment.LOCAL) { // Running locally
      // This is a crude check to make sure we are only pointing to test projects when running
      // locally and connecting to GCP
      Environment projectIdEnvironment = getProjectEnvironment(projectId);
      Preconditions.checkArgument(
          projectIdEnvironment == Environment.LOCAL
              || projectIdEnvironment == Environment.TEST
              || projectIdEnvironment == Environment.QA,
          "Invalid project to connect to with env=LOCAL. "
              + projectId
              + " doesn't appear to"
              + " be a local/test project since it doesn't end in -local, -test, or -qa.");
    } else { // Assume running on GCP
      // TODO: Check whether we are actually running on GCP once we find out how
      String credsLocation = System.getenv(GCP_CREDENTIALS_PATH_ENV_VAR);
      if (!credsLocation.startsWith(KUBERNETES_SECRETS_PATH_ROOT)) {
        String cause =
            String.format(
                "You are attempting to obtain credentials from somewhere "
                    + "other than Kubernetes secrets in prod. You may have accidentally copied creds "
                    + "into your image, which we provide as a local debugging mechanism only. See GCP "
                    + "build script (config/gcp/build_and_upload_docker_image.sh) for more info. Creds "
                    + "location was: %s",
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
  @Provides
  @Singleton
  @ProjectId
  String getProjectId(ExtensionContext context) {
    validateUsingGoogle(context);
    String projectId;
    try {
      projectId = System.getenv(GCP_PROJECT_ID_ENV_VAR);
    } catch (NullPointerException e) {
      throw new IllegalArgumentException(
          "Need to specify a project ID when using Google Cloud. "
              + "This should be exposed as an environment variable by Kubernetes, see "
              + "k8s/api-deployment.yaml");
    }
    Preconditions.checkArgument(
        !Strings.isNullOrEmpty(projectId),
        "Need to specify a project "
            + "ID when using Google Cloud. This should be exposed as an environment variable by "
            + "Kubernetes, see k8s/api-deployment.yaml");
    return projectId;
  }

  @Provides
  HttpTransport getHttpTransport() {
    return new NetHttpTransport();
  }

  @Provides
  JsonFactory getJsonFactory() {
    return new JacksonFactory();
  }

  /**
   * Validate we are using Google Cloud. Should be called in all Providers in this module.
   *
   * <p>This allows us to install this module and rely on Guice-ified flag parsing, and not do a
   * separate parse to conditionally install this module.
   *
   * <p>TODO: Can this be removed? In the new modular structure, will this code only be run/loaded
   * for Google cloud?
   */
  private void validateUsingGoogle(ExtensionContext context) {
    // TODO: "cloud" should be a global flag name once we decide the proper place(s) for those
    if (!context.getConfiguration("cloud", "").equals(GOOGLE_CLOUD_NAME)) {
      throw new IllegalStateException("Injecting Google objects when cloud != Google!");
    }
  }

  // TODO(rtannenbaum): Consolidate with core Environment enum once that is defined in new dirs
  @VisibleForTesting
  enum Environment {
    LOCAL,
    TEST,
    QA,
    PROD
  }

  @BindingAnnotation
  @Target({FIELD, PARAMETER, METHOD})
  @Retention(RUNTIME)
  public @interface ProjectId {}
}
