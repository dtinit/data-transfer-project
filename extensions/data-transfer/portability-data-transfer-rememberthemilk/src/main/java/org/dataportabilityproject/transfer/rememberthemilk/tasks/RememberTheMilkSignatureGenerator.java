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
package org.dataportabilityproject.transfer.rememberthemilk.tasks;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;

/**
 * Generates signatures hash based on the algorithm described:
 * https://www.rememberthemilk.com/services/api/authentication.rtm
 */
final class RememberTheMilkSignatureGenerator {

  private final AppCredentials appCredentials;
  private final String authToken;

  // Auth Token is required for the Signature Generator when created within the data transfer
  public RememberTheMilkSignatureGenerator(AppCredentials appCredentials, String authToken) {
    this.appCredentials = Preconditions.checkNotNull(appCredentials);
    Preconditions.checkArgument(!Strings.isNullOrEmpty(authToken));
    this.authToken = authToken;
  }

  public URL getSignature(String base, Map<String, String> queryParams) {
    // Add the RTM specific query params to the map for signing
    Map<String, String> modifiedParams = new HashMap<>();
    modifiedParams.putAll(queryParams);
    modifiedParams.put("api_key", appCredentials.getKey());
    modifiedParams.put("auth_token", authToken);

    List<String> orderedKeys = modifiedParams.keySet().stream().collect(Collectors.toList());
    Collections.sort(orderedKeys);

    List<String> queryParamStrings = new ArrayList<>();
    StringBuilder resultBuilder = new StringBuilder();

    resultBuilder.append(appCredentials.getSecret());
    for (String key : orderedKeys) {
      // trim all keys and values from whitespace - We don't want to escape all whitespace values,
      // because the RTM endpoint will generate the signature with the unescaped whitespace and
      // compare that to the signature generated.
      String k = key.trim();
      String v = modifiedParams.get(key).trim();

      resultBuilder.append(k).append(v);
      queryParamStrings.add(k + "=" + v);
    }

    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      byte[] thedigest = md.digest(resultBuilder.toString().getBytes(StandardCharsets.UTF_8));
      String signature = BaseEncoding.base16().encode(thedigest).toLowerCase();
      return new URL(base + "?" + String.join("&", queryParamStrings) + "&api_sig=" + signature);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Couldn't find MD5 hash", e);
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Couldn't parse authUrl", e);
    }
  }
}
