// (c) Facebook, Inc. and its affiliates. Confidential and proprietary.

package org.datatransferproject.datatransfer.backblaze.common;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class BaseBackblazeS3Client implements BackblazeS3Client {
    private static final String S3_ENDPOINT_FORMAT_STRING = "https://s3.%s.backblazeb2.com";

    public S3Client createS3Client(String accessKey, String secretKey, String region) {
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