/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.datatransfer.backblaze.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.exception.BackblazeCredentialsException;
import org.datatransferproject.transfer.JobMetadata;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

public class BackblazeDataTransferClient {
  private static final String DATA_TRANSFER_BUCKET_PREFIX_FORMAT_STRING = "%s-data-transfer";
  private static final String S3_ENDPOINT_FORMAT_STRING = "https://s3.%s.backblazeb2.com";
  private static final int MAX_BUCKET_CREATION_ATTEMPTS = 10;
  private final List<String> BACKBLAZE_REGIONS =
      Arrays.asList("us-west-000", "us-west-001", "us-west-002", "eu-central-003");

  private static final long SIZE_THRESHOLD_FOR_MULTIPART_UPLOAD = 20 * 1024 * 1024; // 20 MB.
  private static final long PART_SIZE_FOR_MULTIPART_UPLOAD = 5 * 1024 * 1024; // 5 MB.

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

    // The Key ID starts with the region identifier number, so reorder the regions such that
    // the first region is most likely the user's region
    String regionId = keyId.substring(0, 3);
    BACKBLAZE_REGIONS.sort(
        (String region1, String region2) -> {
          if (region1.endsWith(regionId)) {
            return -1;
          }
          return 0;
        });

    Throwable s3Exception = null;
    for (String region : BACKBLAZE_REGIONS) {
      try {
        s3Client = getOrCreateS3Client(keyId, applicationKey, region);

        listBucketsResponse = s3Client.listBuckets();
        userRegion = region;
        break;
      } catch (S3Exception e) {
        s3Exception = e;
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
          "User's credentials or permissions are not valid for any regions available", s3Exception);
    }

    bucketName = getOrCreateBucket(s3Client, listBucketsResponse, userRegion);
  }

  public String uploadFile(String fileKey, File file) throws IOException {
    if (s3Client == null || bucketName == null) {
      throw new IllegalStateException("BackblazeDataTransferClient has not been initialised");
    }

    try {
      long contentLength = file.length();
      monitor.debug(
          () -> String.format("Uploading '%s' with file size %d bytes", fileKey, contentLength));

      if (contentLength >= SIZE_THRESHOLD_FOR_MULTIPART_UPLOAD) {
        monitor.debug(
            () ->
                String.format(
                    "File size is larger than %d bytes, so using multipart upload",
                    SIZE_THRESHOLD_FOR_MULTIPART_UPLOAD));
        return uploadFileUsingMultipartUpload(fileKey, file, contentLength);
      }

      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder().bucket(bucketName).key(fileKey).build();

      PutObjectResponse putObjectResponse =
          s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));

      return putObjectResponse.versionId();
    } catch (AwsServiceException | SdkClientException e) {
      throw new IOException(String.format("Error while uploading file, fileKey: %s", fileKey), e);
    }
  }

  private String uploadFileUsingMultipartUpload(String fileKey, File file, long contentLength)
      throws IOException, AwsServiceException, SdkClientException {
    List<CompletedPart> completedParts = new ArrayList<>();

    CreateMultipartUploadRequest createMultipartUploadRequest =
        CreateMultipartUploadRequest.builder().bucket(bucketName).key(fileKey).build();
    CreateMultipartUploadResponse createMultipartUploadResponse =
        s3Client.createMultipartUpload(createMultipartUploadRequest);

    long filePosition = 0;
    try (InputStream fileInputStream = new FileInputStream(file)) {
      for (int i = 1; filePosition < contentLength; i++) {
        // Because the last part could be smaller than others, adjust the part size as needed
        long partSize = Math.min(PART_SIZE_FOR_MULTIPART_UPLOAD, (contentLength - filePosition));

        UploadPartRequest uploadRequest =
            UploadPartRequest.builder()
                .bucket(bucketName)
                .key(fileKey)
                .uploadId(createMultipartUploadResponse.uploadId())
                .partNumber(i)
                .build();
        RequestBody requestBody = RequestBody.fromInputStream(fileInputStream, partSize);

        UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadRequest, requestBody);
        completedParts.add(
            CompletedPart.builder().partNumber(i).eTag(uploadPartResponse.eTag()).build());

        filePosition += partSize;
      }
    }

    CompleteMultipartUploadRequest completeMultipartUploadRequest =
        CompleteMultipartUploadRequest.builder()
            .bucket(bucketName)
            .key(fileKey)
            .uploadId(createMultipartUploadResponse.uploadId())
            .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
            .build();

    CompleteMultipartUploadResponse completeMultipartUploadResponse =
        s3Client.completeMultipartUpload(completeMultipartUploadRequest);

    return completeMultipartUploadResponse.versionId();
  }

  private String getOrCreateBucket(
      S3Client s3Client, ListBucketsResponse listBucketsResponse, String region)
      throws IOException {

    String fullPrefix =
        String.format(
            DATA_TRANSFER_BUCKET_PREFIX_FORMAT_STRING,
            JobMetadata.getExportService().toLowerCase());
    try {
      for (Bucket bucket : listBucketsResponse.buckets()) {
        if (bucket.name().startsWith(fullPrefix)) {
          return bucket.name();
        }
      }

      for (int i = 0; i < MAX_BUCKET_CREATION_ATTEMPTS; i++) {
        String bucketName =
            String.format("%s-%s", fullPrefix, RandomStringUtils.randomNumeric(8).toLowerCase());
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
          monitor.info(() -> "Bucket name already exists");
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

  private S3Client getOrCreateS3Client(String accessKey, String secretKey, String region) {
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
