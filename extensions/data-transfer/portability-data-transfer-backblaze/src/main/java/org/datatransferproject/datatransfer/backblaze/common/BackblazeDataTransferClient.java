package org.datatransferproject.datatransfer.backblaze.common;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.exception.BackblazeCredentialsException;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

public class BackblazeDataTransferClient {
  private static final String DATA_TRANSFER_BUCKET_PREFIX = "facebook-data-transfer";
  private static final String S3_ENDPOINT_FORMAT_STRING = "https://s3.%s.backblazeb2.com";
  private static final int MAX_BUCKET_CREATION_ATTEMPTS = 10;
  private static final List<String> BACKBLAZE_REGIONS =
      Arrays.asList("us-west-000", "us-west-001", "us-west-002", "eu-central-003");

  private final Monitor monitor;
  private S3Client s3Client;
  private String bucketName;

  public BackblazeDataTransferClient(Monitor monitor) {
    this.monitor = monitor;
  }

  public void init(String keyId, String applicationKey)
      throws BackblazeCredentialsException, IOException {
    // Fetch all the available buckets and use that to find which region the user is in
    ListBucketsResponse listBucketsResponse = null;
    String userRegion = null;
    for (String region : BACKBLAZE_REGIONS) {
      try {
        s3Client = getOrCreateS3Client(keyId, applicationKey, region);

        listBucketsResponse = s3Client.listBuckets();
        userRegion = region;
        break;
      } catch (S3Exception e) {
        if (s3Client != null) {
          s3Client.close();
        }
        if (e.statusCode() == 403) {
          monitor.debug(() -> String.format("User is not in region %s", region));
        }
      }
    }

    if (listBucketsResponse == null || userRegion == null) {
      throw new BackblazeCredentialsException(
          "User's credentials or permissions are not valid for any regions available");
    }

    bucketName = getOrCreateBucket(s3Client, listBucketsResponse, userRegion);
  }

  public String uploadFile(String fileKey, File file) throws IOException {
    if (s3Client == null || bucketName == null) {
      throw new IllegalStateException("BackblazeDataTransferClient has not been initialised");
    }

    try {
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder().bucket(bucketName).key(fileKey).build();

      PutObjectResponse putObjectResponse =
          s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

      return putObjectResponse.versionId();
    } catch (AwsServiceException | SdkClientException e) {
      throw new IOException("Error while uploading file", e);
    }
  }

  private static String getOrCreateBucket(
      S3Client s3Client, ListBucketsResponse listBucketsResponse, String region)
      throws IOException {
    try {
      for (Bucket bucket : listBucketsResponse.buckets()) {
        if (bucket.name().startsWith(DATA_TRANSFER_BUCKET_PREFIX)) {
          return bucket.name();
        }
      }

      for (int i = 0; i < MAX_BUCKET_CREATION_ATTEMPTS; i++) {
        String bucketName =
            String.format(
                "%s-%s",
                DATA_TRANSFER_BUCKET_PREFIX, RandomStringUtils.randomAlphanumeric(8).toLowerCase());
        try {
          CreateBucketConfiguration createBucketConfiguration =
              CreateBucketConfiguration.builder().locationConstraint(region).build();
          CreateBucketRequest createBucketRequest =
              CreateBucketRequest.builder()
                  .bucket(bucketName)
                  .createBucketConfiguration(createBucketConfiguration)
                  .build();
          s3Client.createBucket(createBucketRequest);
          return bucketName;
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
          System.out.print("Bucket name already exists");
        }
      }
      throw new IOException(
          String.format(
              "Failed to create a uniquely named bucket after %d attempts",
              MAX_BUCKET_CREATION_ATTEMPTS));

    } catch (AwsServiceException | SdkClientException e) {
      throw new IOException("Error while creating bucket", e);
    }
  }

  private static S3Client getOrCreateS3Client(String accessKey, String secretKey, String region) {
    AwsSessionCredentials awsCreds = AwsSessionCredentials.create(accessKey, secretKey, "");

    ClientOverrideConfiguration clientOverrideConfiguration =
        ClientOverrideConfiguration.builder().putHeader("User-Agent", "Facebook-DTP").build();

    // Use any AWS region for the client, the Backblaze API does not care about it
    Region awsRegion = Region.US_EAST_1;

    return S3Client.builder()
        .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
        .overrideConfiguration(clientOverrideConfiguration)
        .endpointOverride(URI.create(String.format(S3_ENDPOINT_FORMAT_STRING, region)))
        .region(awsRegion)
        .build();
  }
}
