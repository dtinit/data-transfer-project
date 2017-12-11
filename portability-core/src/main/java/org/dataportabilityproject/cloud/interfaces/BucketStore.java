package org.dataportabilityproject.cloud.interfaces;

/**
 * Object storage in buckets.
 */
public interface BucketStore {
  byte[] getBlob(String bucketName, String blobName);
}
