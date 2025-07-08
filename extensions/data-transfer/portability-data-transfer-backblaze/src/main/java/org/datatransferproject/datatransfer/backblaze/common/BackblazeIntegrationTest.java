package org.datatransferproject.datatransfer.backblaze.common;

import org.apache.http.impl.client.HttpClientBuilder;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.launcher.monitor.ConsoleMonitor;

import java.io.File;

public class BackblazeIntegrationTest {
    public static void main(String[] args) throws Exception {
        // Create dependencies
        Monitor monitor = new ConsoleMonitor(ConsoleMonitor.Level.DEBUG);
        BaseBackblazeS3ClientFactory factory = new BaseBackblazeS3ClientFactory();

        // Create client with appropriate thresholds
        // 20MB threshold for multipart upload, 5MB part size
        BackblazeDataTransferClient client = new BackblazeDataTransferClient(
                monitor,
                factory,
                20 * 1024 * 1024,
                5 * 1024 * 1024
        );

        // Get credentials from environment
        String keyId = System.getenv("BACKBLAZE_KEY");
        String appKey = System.getenv("BACKBLAZE_SECRET");

        if (keyId == null || appKey == null) {
            System.err.println("Please set BACKBLAZE_KEY and BACKBLAZE_SECRET environment variables");
            return;
        }

        System.out.println("Initializing client with credentials...");

        // Initialize client with your credentials
        // The "test-service" string is used as a prefix for bucket naming
        client.init(keyId, appKey, "test-service", HttpClientBuilder.create().build());

        System.out.println("Client initialized successfully!");

        // Test file upload
        File testFile = new File("/Users/anthony.ross/Desktop/test.txt"); // Replace with an actual file path

        System.out.println("Uploading file: " + testFile.getAbsolutePath());
        String versionId = client.uploadFile("test-upload-" + System.currentTimeMillis(), testFile);

        System.out.println("Upload successful! Version ID: " + versionId);
    }
}
