package org.dataportabilityproject.cloud.google;

import com.google.inject.Inject;

/**
 * Wrapper class for a Google Cloud Platform project ID.
 *
 * <p>Injecting @ProjectId into Google cloud implementation classes is an alternative to injecting
 * @SomeProjectIdAnnotation String projectId, that prevents Guice from eagerly evaluating
 * Provider<GoogleCloudFactory> when we are not using Google Cloud.
 * TODO: Investigate injecting @SomeProjectIdAnnotation String projectId and not eagerly evaluating
 * Provider<GoogleCloudFactory>
 */
class ProjectId {
  private final String projectId;

  @Inject
  public ProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getProjectId() {
    return projectId;
  }
}
