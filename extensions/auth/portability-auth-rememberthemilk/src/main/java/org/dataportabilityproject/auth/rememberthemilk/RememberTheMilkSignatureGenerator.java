/*
 * Copyright 2018 The Data Transfer Project Authors.
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
package org.dataportabilityproject.auth.rememberthemilk;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
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
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

/**
 * Generates signatures hash based on the algorithm described:
 * https://www.rememberthemilk.com/services/api/authentication.rtm
 */
public final class RememberTheMilkSignatureGenerator {

  private final AppCredentials appCredentials;

  public RememberTheMilkSignatureGenerator(AppCredentials appCredentials) {
    this.appCredentials = Preconditions.checkNotNull(appCredentials);
  }

  public URL getSignature(URL url) throws MalformedURLException {
    String query = url.getQuery();
    Map<String, String> map =
        new HashMap<>(Splitter.on('&').withKeyValueSeparator("=").split(query));

    String apiKey = appCredentials.getKey();
    String secret = appCredentials.getSecret();

    map.put("api_key", apiKey);

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
      return new URL(url + "&api_key=" + apiKey + "&api_sig=" + signature);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Couldn't find MD5 hash", e);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Couldn't parse authUrl", e);
    }
  }
}
