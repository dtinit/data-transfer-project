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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
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
  private HttpClient httpClient;

  public BackblazeDataTransferClient(
      Monitor monitor,
      BackblazeS3ClientFactory backblazeS3ClientFactory,
      HttpClient httpClient,
      long sizeThresholdForMultipartUpload,
      long partSizeForMultiPartUpload) {
    this.monitor = monitor;
    this.backblazeS3ClientFactory = backblazeS3ClientFactory;
    this.httpClient = httpClient;
    // Avoid infinite loops
    if (partSizeForMultiPartUpload <= 0)
      throw new IllegalArgumentException("Part size for multipart upload must be positive.");
    this.sizeThresholdForMultipartUpload = sizeThresholdForMultipartUpload;
    this.partSizeForMultiPartUpload = partSizeForMultiPartUpload;
  }

  public void init(String keyId, String applicationKey, String exportService)
      throws BackblazeCredentialsException, IOException {
    // Fetch all the available buckets and use that to find which region the user is in
    ListBucketsResponse listBucketsResponse = null;

    Throwable s3Exception = null;
    final String userRegion = getAccountRegion(httpClient, keyId, applicationKey);
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

  /**
   * Retrieves the account region from Backblaze B2 API using the provided credentials. This method
   * implements exponential backoff retry logic for handling transient server errors.
   *
   * @param httpClient The HTTP client to use for making the API request
   * @param keyId The Backblaze B2 account key ID
   * @param applicationKey The Backblaze B2 application key
   * @return The region string extracted from the s3ApiUrl in the response
   * @throws BackblazeCredentialsException if: - Authentication fails (4xx errors) - Server errors
   *     persist after all retry attempts - Response parsing fails - Network errors occur and
   *     persist after retries
   */
  private String getAccountRegion(HttpClient httpClient, String keyId, String applicationKey)
      throws BackblazeCredentialsException {

    String auth = keyId + ":" + applicationKey;
    byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
    String authHeaderValue = "Basic " + new String(encodedAuth);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create("https://api.backblazeb2.com/b2api/v2/b2_authorize_account"))
            .header("Authorization", authHeaderValue)
            .GET()
            .build();

    final int maxRetries = 3;
    final long initialDelayMs = 1000; // 1 second
    int attempts = 0;
    Exception lastException = null;

    while (attempts < maxRetries) {
      try {
        if (attempts > 0) {
          // Calculate exponential backoff delay
          long delayMs = initialDelayMs * (long) Math.pow(2, attempts - 1);
          int finalAttempts = attempts;
          monitor.info(
              () -> String.format("Retry attempt %d, waiting %d ms", finalAttempts + 1, delayMs));
          Thread.sleep(delayMs);
        }

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
          String responseBody = response.body();
          JSONParser parser = new JSONParser();
          JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);
          String s3ApiUrl = (String) jsonResponse.get("s3ApiUrl");
          String region = s3ApiUrl.split("s3.")[1].split("\\.")[0];
          monitor.info(() -> "Region extracted from s3ApiUrl: " + region);
          return region;
        } else if (response.statusCode() >= 500) {
          // Retry on server errors
          monitor.info(
              () -> String.format("Received server error %d, will retry", response.statusCode()));
          lastException =
              new BackblazeCredentialsException(
                  "Failed to retrieve users region. Status code: " + response.statusCode(), null);
        } else {
          // Don't retry on client errors (4xx)
          throw new BackblazeCredentialsException(
              "Failed to retrieve users region. Status code: " + response.statusCode(), null);
        }

      } catch (IOException | InterruptedException | ParseException e) {
        monitor.info(() -> String.format("Request failed with error: %s", e.getMessage()));
        lastException = e;
      }

      attempts++;
    }

    // If we've exhausted all retries, throw the last exception
    throw new BackblazeCredentialsException(
        "Failed to retrieve users region after " + maxRetries + " attempts", lastException);
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
}
