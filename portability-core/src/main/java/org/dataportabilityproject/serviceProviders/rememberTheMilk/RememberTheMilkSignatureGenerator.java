package org.dataportabilityproject.serviceProviders.rememberTheMilk;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Generates signatures hash based on the algorithm described:
 * https://www.rememberthemilk.com/services/api/authentication.rtm
 */
final class RememberTheMilkSignatureGenerator {
    private final String secret;
    private final String apiKey;
    private final String authToken;

    RememberTheMilkSignatureGenerator(String apiKey, String secret, @Nullable String authToken) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(apiKey), "apiKey can't be null");
        this.apiKey = apiKey;
        Preconditions.checkArgument(!Strings.isNullOrEmpty(secret), "secret can't be null");
        this.secret = secret;
        this.authToken = authToken;
    }

    URL getSignature(URL url) throws MalformedURLException {
        String query = url.getQuery();
        Map<String, String> map = new HashMap<>(Splitter.on('&')
            .trimResults()
            .withKeyValueSeparator("=")
            .split(query));
        map.put("api_key", apiKey);
        if (null != authToken) {
            map.put("auth_token", authToken);
        }
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
            String signature = BaseEncoding.base16().encode(thedigest).toLowerCase();
            return new URL(url
                + "&" + "api_key" + "=" + apiKey
                + (authToken == null ? "" : ("&" + "auth_token" + "=" + authToken))
                + "&" + "api_sig" + "=" + signature);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Couldn't find MD5 hash", e);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Couldn't parse url", e);
        }
    }
}
