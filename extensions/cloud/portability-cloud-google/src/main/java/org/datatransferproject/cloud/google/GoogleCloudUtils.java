package org.datatransferproject.cloud.google;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;

public class GoogleCloudUtils {

    private GoogleCloudUtils() {
    }

    // Environment variable where GCP project ID is stored. The value is set in
    // config/k8s/api-deployment.yaml.
    private static final String GCP_PROJECT_ID_ENV_VAR = "GOOGLE_PROJECT_ID";

    /**
     * Creates an Entity Builder for the given key and properties. Converts the objects to the proper
     * datastore values
     */
    static Entity.Builder createEntityBuilder(Key key, Map<String, Object> data) throws IOException {
        Entity.Builder builder = Entity.newBuilder(key);
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() instanceof String) {
                // StringValue
                builder.set(entry.getKey(), (String) entry.getValue());
            } else if (entry.getValue() instanceof Integer) {
                // LongValue
                builder.set(entry.getKey(), (Integer) entry.getValue());
            } else if (entry.getValue() instanceof Double) {
                // DoubleValue
                builder.set(entry.getKey(), (Double) entry.getValue());
            } else if (entry.getValue() instanceof Boolean) {
                // BooleanValue
                builder.set(entry.getKey(), (Boolean) entry.getValue());
            } else if (entry.getValue() instanceof Timestamp) {
                // TimestampValue
                builder.set(entry.getKey(), (Timestamp) entry.getValue());
            } else {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
                    out.writeObject(entry.getValue());
                }
                // BlobValue
                builder.set(entry.getKey(), Blob.copyFrom(bos.toByteArray()));
            }
        }
        return builder;
    }

    /**
     * Returns the currently initialized project id based on the System env setup
     */
    static String getProjectId() {
        return System.getenv(GCP_PROJECT_ID_ENV_VAR);
    }
}
