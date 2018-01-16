package org.dataportabilityproject.cloud.google;

import com.google.inject.Inject;

/**
 * Wrapper class for a Google Cloud Platform project ID.
 *
 * <p>This class exists so that we can inject ProjectId into Google implementation classes without
 * forcing Guice to eagerly eval Provider<GoogleCloudFactory> when not using cloud=GOOGLE. i.e. if
 * we just bound the project ID as a String directly ("@ProjectId String projectId") via
 * bind(String.toClass).annotatedWith(ProjectId.class).toInstance(...), that causes eager init.
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
