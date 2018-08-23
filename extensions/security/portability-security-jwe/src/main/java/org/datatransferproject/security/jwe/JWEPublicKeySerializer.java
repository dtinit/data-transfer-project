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
package org.datatransferproject.security.jwe;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import org.datatransferproject.spi.transfer.security.PublicKeySerializer;
import org.datatransferproject.spi.transfer.security.SecurityException;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/** */
public class JWEPublicKeySerializer implements PublicKeySerializer {

  @Override
  public boolean canHandle(String scheme) {
    return "jwe".equals(scheme);
  }

  @Override
  public String serialize(PublicKey publicKey) throws SecurityException {
    String kid = UUID.randomUUID().toString();
    JWK jwk = new RSAKey.Builder((RSAPublicKey) publicKey).keyID(kid).build();
    return jwk.toString();
  }
}
