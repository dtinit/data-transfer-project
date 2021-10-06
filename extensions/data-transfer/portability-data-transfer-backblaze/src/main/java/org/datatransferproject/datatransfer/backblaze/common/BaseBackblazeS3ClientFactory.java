/*
 * Copyright 2021 The Data Transfer Project Authors.
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

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

public class BaseBackblazeS3ClientFactory implements BackblazeS3ClientFactory {
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
