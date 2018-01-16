package org.dataportabilityproject.cloud.google;

import com.google.inject.Inject;

/**
 * Wrapper class for a Google Cloud Platform project ID.
 *
 * <p>Injecting "ProjectId projectId" into Google cloud implementation classes is an alternative to
 * injecting "@ProjectId String projectId", that prevents Guice from eagerly evaluating
 * Provider<GoogleCloudFactory> when we are not using Google Cloud.
 * TODO: Investigate injecting "@ProjectId String projectId" and not eagerly evaluating, then we
 * won't need this wrapper class.
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
