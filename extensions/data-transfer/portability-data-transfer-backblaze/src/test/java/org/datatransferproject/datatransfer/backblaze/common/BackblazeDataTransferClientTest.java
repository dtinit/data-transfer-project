// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.datatransfer.backblaze.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.exception.BackblazeCredentialsException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;

@RunWith(MockitoJUnitRunner.class)
public class BackblazeDataTransferClientTest {
    @Mock private Monitor monitor;
    @Mock private BackblazeS3Client backblazeS3Client;
    @Mock private S3Client s3Client;
    private static File testFile;
    private static String KEY_ID = "keyId";
    private static String APP_KEY = "appKey";
    private static String EXPORT_SERVICE = "exp-serv";
    private static String FILE_KEY = "fileKey";
    private static String VALID_BUCKET_NAME = EXPORT_SERVICE + "-data-transfer-bucket";

    @BeforeClass
    public static void setUpClass() {
        String prefix = "fbjava/data-transfer-project/portability-data-transfer-backblaze/";
        testFile = new File(prefix + "src/test/resources/test.txt");
    }

    @Before
    public void setUp() {
        reset(monitor);
        reset(backblazeS3Client);
        reset(s3Client);
        when(backblazeS3Client.createS3Client(anyString(), anyString(), anyString()))
                .thenReturn(s3Client);
    }

    private void createValidBucketList() {
        Bucket bucket = Bucket.builder().name(VALID_BUCKET_NAME).build();
        when(s3Client.listBuckets()).thenReturn(ListBucketsResponse.builder().buckets(bucket).build());
    }

    private void createEmptyBucketList() {
        when(s3Client.listBuckets()).thenReturn(ListBucketsResponse.builder().build());
    }

    private BackblazeDataTransferClient createDefaultClient() {
        return new BackblazeDataTransferClient(monitor, backblazeS3Client, 1000, 500);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongPartSize() {
        // Act
        new BackblazeDataTransferClient(monitor, backblazeS3Client, 10, 0);
        // Assert: expected exception
    }

    @Test
    public void testInitBucketNameMatches() throws BackblazeCredentialsException, IOException {
        // Arrange
        createValidBucketList();
        BackblazeDataTransferClient client = createDefaultClient();
        // Act
        client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        // Assert: no bucket is created
        verify(s3Client, times(0)).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    public void testInitBucketCreated() throws BackblazeCredentialsException, IOException {
        // Arrange
        Bucket bucket = Bucket.builder().name("invalid-name").build();
        when(s3Client.listBuckets()).thenReturn(ListBucketsResponse.builder().buckets(bucket).build());
        BackblazeDataTransferClient client = createDefaultClient();
        // Act
        client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        // Assert: bucket created once
        verify(s3Client, times(1)).createBucket(any(CreateBucketRequest.class));
    }

    @Test(expected = IOException.class)
    public void testInitBucketNameExists() throws BackblazeCredentialsException, IOException {
        // Arrange
        createEmptyBucketList();
        when(s3Client.createBucket(any(CreateBucketRequest.class)))
                .thenThrow(BucketAlreadyExistsException.builder().build());
        BackblazeDataTransferClient client = createDefaultClient();
        // Act
        try {
            client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        } catch (Exception ex) {
            // Assert
            verify(monitor, atLeast(1)).info(any());
            throw ex;
        }
    }

    @Test(expected = IOException.class)
    public void testInitErrorCreatingBucket() throws BackblazeCredentialsException, IOException {
        // Arrange
        createEmptyBucketList();
        when(s3Client.createBucket(any(CreateBucketRequest.class)))
                .thenThrow(AwsServiceException.builder().build());
        BackblazeDataTransferClient client = createDefaultClient();
        // Act
        client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        // Assert: expected exception
    }

    @Test(expected = BackblazeCredentialsException.class)
    public void testInitListBucketException() throws BackblazeCredentialsException, IOException {
        // Arrange
        when(s3Client.listBuckets()).thenThrow(S3Exception.builder().statusCode(403).build());
        BackblazeDataTransferClient client = createDefaultClient();
        // Act
        try {
            client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        } catch (Exception ex) {
            // Assert: closed client
            verify(s3Client, atLeast(1)).close();
            verify(monitor, atLeast(1)).debug(any());
            throw ex;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testUploadFileNonInitialized() throws IOException {
        // Arrange
        BackblazeDataTransferClient client = createDefaultClient();
        // Act
        client.uploadFile(FILE_KEY, testFile);
        // Assert: expected exception
    }

    @Test
    public void testUploadFileSingle() throws BackblazeCredentialsException, IOException {
        // Arrange
        final String expectedVersionId = "123";
        createValidBucketList();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().versionId(expectedVersionId).build());
        BackblazeDataTransferClient client = createDefaultClient();
        client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        // Act
        String actualVersionId = client.uploadFile(FILE_KEY, testFile);
        // Assert
        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals(expectedVersionId, actualVersionId);
    }

    @Test(expected = IOException.class)
    public void testUploadFileSingleException() throws BackblazeCredentialsException, IOException {
        // Arrange
        createValidBucketList();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(AwsServiceException.builder().build());
        BackblazeDataTransferClient client = createDefaultClient();
        client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        // Act
        client.uploadFile(FILE_KEY, testFile);
        // Assert: expected exception
    }

    @Test
    public void testUploadFileMultipart() throws BackblazeCredentialsException, IOException {
        // Arrange
        final String expectedVersionId = "123";
        createValidBucketList();
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("xyz").build());
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenReturn(UploadPartResponse.builder().build());
        when(s3Client.completeMultipartUpload(any(CompleteMultipartUploadRequest.class)))
                .thenReturn(CompleteMultipartUploadResponse.builder().versionId(expectedVersionId).build());
        final long partSize = 10;
        final long fileSize = testFile.length();
        final long expectedParts = fileSize / partSize + (fileSize % partSize == 0 ? 0 : 1);
        BackblazeDataTransferClient client =
                new BackblazeDataTransferClient(monitor, backblazeS3Client, fileSize / 2, partSize);
        client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        // Act
        String actualVersionId = client.uploadFile(FILE_KEY, testFile);
        // Assert: uploaded in 8 parts
        verify(s3Client, times((int) expectedParts))
                .uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
        assertEquals(expectedVersionId, actualVersionId);
    }

    @Test(expected = IOException.class)
    public void testUploadFileMultipartException() throws BackblazeCredentialsException, IOException {
        // Arrange
        createValidBucketList();
        when(s3Client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
                .thenReturn(CreateMultipartUploadResponse.builder().uploadId("xyz").build());
        when(s3Client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
                .thenThrow(AwsServiceException.builder().build());
        final long fileSize = testFile.length();
        BackblazeDataTransferClient client =
                new BackblazeDataTransferClient(monitor, backblazeS3Client, fileSize / 2, fileSize / 8);
        client.init(KEY_ID, APP_KEY, EXPORT_SERVICE);
        // Act
        client.uploadFile(FILE_KEY, testFile);
        // Assert: expected exception
    }
}