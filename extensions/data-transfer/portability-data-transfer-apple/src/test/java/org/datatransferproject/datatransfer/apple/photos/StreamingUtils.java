/*
 * Copyright 2023 The Data Transfer Project Authors.
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
package org.datatransferproject.datatransfer.apple.photos;

import java.io.IOException;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.apple.exceptions.AppleContentException;
import org.datatransferproject.datatransfer.apple.photos.streaming.StreamingContentClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

public class StreamingUtils {

    public static void validateContent(@NotNull final String expectedUrl, @Nullable final String actualUrl, @Nullable final int contentRequestLength) throws IOException, AppleContentException {
        final Monitor monitor = new Monitor() {
        };
        final StreamingContentClient expectedClient = new StreamingContentClient(expectedUrl, StreamingContentClient.StreamingMode.DOWNLOAD, monitor);
        final byte[] expectedBytes = expectedClient.downloadBytes(contentRequestLength);
        final StreamingContentClient actualClient = new StreamingContentClient(expectedUrl, StreamingContentClient.StreamingMode.DOWNLOAD, monitor);
        final byte[] actualBytes = actualClient.downloadBytes(contentRequestLength);
        validateContent(expectedBytes, actualBytes);
        // confirm there is no more data and close the connection
        Assert.assertNull(expectedClient.downloadBytes(1));
        Assert.assertNull(actualClient.downloadBytes(1));
    }

    public static void validateContent(@NotNull String url, @Nullable byte[] expectedContent) throws IOException, AppleContentException {
        final Monitor monitor = new Monitor() {
        };
        final StreamingContentClient client = new StreamingContentClient(url, StreamingContentClient.StreamingMode.DOWNLOAD, monitor);
        if (expectedContent != null) {
            byte[] actualContent = client.downloadBytes(expectedContent.length);
            validateContent(expectedContent, actualContent);
            // confirm there is no more data and close the connection
            Assert.assertNull(client.downloadBytes(1));
        } else {
            Assert.assertNotNull("Got no data back from cvws", client.downloadBytes(1));
        }
    }

    public static void validateContent(@Nullable byte[] expectedContent, @Nullable byte[] actualContent) throws IOException, AppleContentException {
        Assert.assertNotNull("no data in content", actualContent);
        Assert.assertNotNull("no data in content", expectedContent);
        Assert.assertArrayEquals("mismatch content", expectedContent, actualContent);
    }
}
