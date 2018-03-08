package org.dataportabilityproject.cloud.google;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.BucketStore;
import org.dataportabilityproject.spi.cloud.storage.CryptoKeyStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleCloudExtension implements CloudExtension {
  // TODO: Should these be initialized from the ExtensionContext and not injected?
  // If injected, this requires that we install the GoogleCloudModule before using which might not
  // make sense depending on where it would happen
  private final GoogleCredentials googleCredentials;
  private final String projectId;
  private final HttpTransport transport;
  private final JsonFactory jsonFactory;
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudExtension.class);

  private boolean initialized = false;
  private Datastore datastore;
  private JobStore jobstore;
  private BucketStore bucketStore;
  private GoogleCryptoKeyStore cryptoKeyManagementSystem;

  @Inject
  GoogleCloudExtension(
      @GoogleCloudModule.ProjectId String projectId,
      GoogleCredentials googleCredentials,
      HttpTransport transport,
      JsonFactory jsonFactory) {
    this.projectId = projectId;
    this.googleCredentials = googleCredentials;
    this.transport = transport;
    this.jsonFactory = jsonFactory;
  }

  @Override
  public JobStore getJobStore() {
    Preconditions.checkArgument(initialized, "Attempting to getJobStore() before initializing");
    return jobstore;
  }

  @Override
  public BucketStore getBucketStore() {
    Preconditions.checkArgument(initialized, "Attempting to getBucketStore() before initializing");
    return bucketStore;
  }

  @Override
  public CryptoKeyStore getCryptoKeyStore() {
    Preconditions.checkArgument(initialized, "Attempting to getCryptoKeyStore() before initializing");
    return cryptoKeyManagementSystem;
  }

  @Override
  public void initialize(ExtensionContext context) {
    this.datastore =
        DatastoreOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(googleCredentials)
            .build()
            .getService();
    this.jobstore = new GoogleJobStore(datastore);
    this.bucketStore = new GoogleBucketStore(googleCredentials,projectId);

    try {
      this.cryptoKeyManagementSystem = new GoogleCryptoKeyStore(transport, jsonFactory, projectId);
    } catch (GoogleCredentialException e) {
      //TODO: the method doesn't throw an exception, how do we pass this onto the user?
      logger.debug("Error creating GoogleCryptoKeyStore: ", e);
    }
    this.initialized = true;
  }

  @Override
  public void start() {}

  @Override
  public void shutdown() {}
}
