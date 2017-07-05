package org.dataportabilityproject.shared;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Holds Api keys and secrets for the various services.
 */
public final class Secrets {
    private final ImmutableMap<String, String> secrets;

    public Secrets(String filePath) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceUrl = classLoader.getResource(filePath);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource " + filePath + " was not found");
        }
        File file = new File(resourceUrl.getFile());

        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        List<String> lines = Files.readLines(file, Charset.forName("UTF-8"));
        for(String line : lines){
            // allow for comments
            String trimmedLine = line.trim();
            if (!trimmedLine.startsWith("//") && !trimmedLine.startsWith("#") && !trimmedLine.isEmpty()) {
                String[] parts = trimmedLine.split(",");
                checkState(parts.length == 2, "Each line should have exactly 2 string seperated by a ,: %s", line);
                builder.put(parts[0].trim(), parts[1].trim());
            }
        }
        this.secrets = builder.build();
    }

    public String get(String key) {
        return secrets.get(key);
    }

    /**
     * Looks up a given key in the secrets file, then uses that value to open a secondary resource
     * file and streams that out.
     **/
    public InputStream getReferencedInputStream(String key) throws FileNotFoundException {
        String path = secrets.get(key);
        checkState(!Strings.isNullOrEmpty(path), "Key %s was not defined in secrets file", key);
        ClassLoader classLoader = getClass().getClassLoader();
        URL resourceUrl = classLoader.getResource(path);
        if (resourceUrl == null) {
            throw new IllegalArgumentException("Resource " + path + " was not found, from key: "
                + key);
        }
        return new FileInputStream(resourceUrl.getFile());
    }
}
