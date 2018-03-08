package org.dataportabilityproject.cloud.google;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.common.base.Preconditions;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.cloud.storage.BucketStore;
import org.dataportabilityproject.spi.cloud.storage.CryptoKeyStore;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GoogleCloudExtension implements CloudExtension {
  private static final Logger logger = LoggerFactory.getLogger(GoogleCloudExtension.class);
  private GoogleCredentials googleCredentials;
  private HttpTransport transport;
  private JsonFactory jsonFactory;
  private Datastore datastore;
  private JobStore jobstore;
  private BucketStore bucketStore;
  private GoogleCryptoKeyStore cryptoKeyManagementSystem;
  private String projectId;
  private boolean initialized = false;

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
    Preconditions.checkArgument(
        initialized, "Attempting to getCryptoKeyStore() before initializing");
    return cryptoKeyManagementSystem;
  }

  @Override
  public void initialize(ExtensionContext context) {
    try {
      projectId = context.getConfiguration("GOOGLE_PROJECT_ID", "");
      Preconditions.checkArgument(!projectId.isEmpty(), "Google project id not found");
      googleCredentials = GoogleCredentials.getApplicationDefault();
      datastore =
          DatastoreOptions.newBuilder()
              .setProjectId(projectId)
              .setCredentials(googleCredentials)
              .build()
              .getService();
      jobstore = new GoogleJobStore(datastore);
      bucketStore = new GoogleBucketStore(googleCredentials, projectId);

      // TODO: Hook these up with the global instances
      transport =  GoogleNetHttpTransport.newTrustedTransport();
      jsonFactory = new JacksonFactory();
      cryptoKeyManagementSystem = new GoogleCryptoKeyStore(transport, jsonFactory, projectId);

      initialized = true;
    } catch (IOException | GoogleCredentialException | GeneralSecurityException e) {
      // TODO: the method doesn't throw an exception, how do we pass this onto the user?
      logger.debug("Error initializing extension: " + this.getClass().getName(), e);
      initialized = false;
    }
  }

  @Override
  public void shutdown() {
    this.initialized = false;
  }
}
