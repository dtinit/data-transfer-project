// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.datatransfer.backblaze.common;

import software.amazon.awssdk.services.s3.S3Client;

public interface BackblazeS3Client {
    S3Client createS3Client(String accessKey, String secretKey, String region);
}