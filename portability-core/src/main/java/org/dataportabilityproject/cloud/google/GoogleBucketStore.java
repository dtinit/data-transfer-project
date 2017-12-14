package org.dataportabilityproject.cloud.google;

import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.dataportabilityproject.cloud.google.GoogleCloudFactory.CredentialsException;
import org.dataportabilityproject.cloud.interfaces.BucketStore;

final class GoogleBucketStore implements BucketStore {
  private Storage storage;

  GoogleBucketStore() throws CredentialsException {
    storage = StorageOptions.newBuilder()
        .setProjectId(GoogleCloudFactory.getGoogleProjectId())
        .setCredentials(GoogleCloudFactory.getCredentials())
        .build().getService();
  }

  public byte[] getBlob(String bucketName, String blobName) {
    Bucket bucket = storage.get(bucketName);
    return bucket.get(blobName).getContent();
  }
}
