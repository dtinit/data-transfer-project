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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.exception.BackblazeCredentialsException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
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

/**
 * Represents a client for handling data transfer operations with Backblaze B2's S3 compatible API.
 * This class is responsible for managing the initialization of connections, uploading files, and
 * handling multipart uploads for large files.
 *
 * <p>The client requires valid Backblaze credentials (keyId and applicationKey) and an instance of
 * a pre-configured S3 client factory for communication with the Backblaze API. Additionally, a
 * Monitor is used to log diagnostic messages during execution.
 *
 * <p>The class provides methods to: - Initialize the BackblazeDataTransferClient using user
 * credentials. - Upload files either as single uploads or using multipart uploads for larger files.
 * - Create or select appropriate buckets for data transfer.
 *
 * <p>The client implements retry mechanisms and proper error handling for common scenarios like
 * network failures or authentication issues.
 */
public class BackblazeDataTransferClient {
  private static final String DATA_TRANSFER_BUCKET_PREFIX_FORMAT_STRING = "%s-data-transfer";
  private static final int MAX_BUCKET_CREATION_ATTEMPTS = 10;

  private final long sizeThresholdForMultipartUpload;
  private final long partSizeForMultiPartUpload;
  private final BackblazeS3ClientFactory backblazeS3ClientFactory;
  private final Monitor monitor;
  private S3Client s3Client;
  private String bucketName;

  public BackblazeDataTransferClient(
      Monitor monitor,
      BackblazeS3ClientFactory backblazeS3ClientFactory,
      long sizeThresholdForMultipartUpload,
      long partSizeForMultiPartUpload) {
    this.monitor = monitor;
    this.backblazeS3ClientFactory = backblazeS3ClientFactory;
    // Avoid infinite loops
    if (partSizeForMultiPartUpload <= 0)
      throw new IllegalArgumentException("Part size for multipart upload must be positive.");
    this.sizeThresholdForMultipartUpload = sizeThresholdForMultipartUpload;
    this.partSizeForMultiPartUpload = partSizeForMultiPartUpload;
  }

  public void init(
      String keyId, String applicationKey, String exportService, CloseableHttpClient httpClient)
      throws BackblazeCredentialsException, IOException {
    // Fetch all the available buckets and use that to find which region the user is in
    ListBucketsResponse listBucketsResponse = null;

    Throwable s3Exception = null;
    String userRegion = getAccountRegion(httpClient, keyId, applicationKey);
    s3Client = backblazeS3ClientFactory.createS3Client(keyId, applicationKey, userRegion);
    try {
      listBucketsResponse = s3Client.listBuckets();
    } catch (S3Exception e) {
      s3Exception = e;
      if (s3Client != null) {
        s3Client.close();
      }
    }

    if (listBucketsResponse == null) {
      throw new BackblazeCredentialsException(
          "User's credentials or permissions are not valid for any regions available", s3Exception);
    }

    bucketName = getOrCreateBucket(s3Client, listBucketsResponse, userRegion, exportService);
  }

  public String uploadFile(String fileKey, File file) throws IOException {
    if (s3Client == null || bucketName == null) {
      throw new IllegalStateException("BackblazeDataTransferClient has not been initialised");
    }

    try {
      long contentLength = file.length();
      monitor.debug(
          () -> String.format("Uploading '%s' with file size %d bytes", fileKey, contentLength));

      if (contentLength >= sizeThresholdForMultipartUpload) {
        monitor.debug(
            () ->
                String.format(
                    "File size is larger than %d bytes, so using multipart upload",
                    sizeThresholdForMultipartUpload));
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

  private String getAccountRegion(
      CloseableHttpClient httpClient, String keyId, String applicationKey)
      throws BackblazeCredentialsException {

    String auth = keyId + ":" + applicationKey;
    byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
    String authHeaderValue = "Basic " + new String(encodedAuth);

    HttpGet request = new HttpGet("https://api.backblazeb2.com/b2api/v2/b2_authorize_account");
    request.addHeader("Authorization", authHeaderValue);

    try {
      CloseableHttpResponse response = httpClient.execute(request);
      try (response) {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 200) {
          String responseBody = EntityUtils.toString(response.getEntity());
          JSONParser parser = new JSONParser();
          JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);
          String s3ApiUrl = (String) jsonResponse.get("s3ApiUrl");
          String region = s3ApiUrl.split("s3.")[1].split("\\.")[0];
          monitor.info(() -> "Region extracted from s3ApiUrl: " + region);
          return region;
        } else if (statusCode >= 400 && statusCode < 500) {
          // Don't retry on client errors (4xx)
          throw new BackblazeCredentialsException(
              "Failed to retrieve account's region. Status code: " + statusCode, null);
        } else {
          throw new IOException("Server returned status code: " + statusCode);
        }
      }
    } catch (IOException | ParseException e) {
      throw new BackblazeCredentialsException("Failed to retrieve users region", e);
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
        long partSize = Math.min(partSizeForMultiPartUpload, (contentLength - filePosition));

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
      S3Client s3Client,
      ListBucketsResponse listBucketsResponse,
      String region,
      String exportService)
      throws IOException {

    String fullPrefix =
        String.format(DATA_TRANSFER_BUCKET_PREFIX_FORMAT_STRING, exportService.toLowerCase());
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
        } catch (Exception e) {
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
}
