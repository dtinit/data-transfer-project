package org.dataportabilityproject.cloud.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.dataportabilityproject.cloud.google.GoogleCloudFactory.CredentialsException;
import org.dataportabilityproject.cloud.interfaces.BucketStore;

final class GoogleBucketStore implements BucketStore {
  private Storage storage;

  GoogleBucketStore() throws CredentialsException {
    GoogleCredentials creds = GoogleCloudFactory.getCredentials();
    storage = StorageOptions.newBuilder()
        .setProjectId(System.getenv("GOOGLE_PROJECT_ID"))
        .setCredentials(creds).build().getService();
  }

  public byte[] getBlob(String bucketName, String blobName) {
    Bucket bucket = storage.get(bucketName);
    return bucket.get(blobName).getContent();
  }
}
