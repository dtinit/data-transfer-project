package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates signatures hash based on the algorithm described:
 * https://www.rememberthemilk.com/services/api/authentication.rtm
 */
public class RememberTheMilkSignatureGenerator {
    private final String secret;

    public RememberTheMilkSignatureGenerator(String secret) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(secret), "secret can't be null");
        this.secret = secret;
    }

    String getSignature(URL url) {
        String query = url.getQuery();
        Map<String, String> map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(query);
        List<String> orderedKeys = map.keySet().stream().collect(Collectors.toList());
        Collections.sort(orderedKeys);

        StringBuilder sb = new StringBuilder(query.length() + secret.length() + 20);
        sb.append(secret);
        for (String key : orderedKeys) {
            sb.append(key).append(map.get(key));
        }

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return BaseEncoding.base16().encode(thedigest).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Couldn't find MD5 hash", e);
        }
    }
}
