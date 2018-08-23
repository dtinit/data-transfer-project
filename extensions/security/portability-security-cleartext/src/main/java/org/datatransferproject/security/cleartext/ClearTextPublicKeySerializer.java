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
package org.datatransferproject.security.cleartext;

import com.google.common.io.BaseEncoding;
import org.datatransferproject.spi.transfer.security.PublicKeySerializer;
import org.datatransferproject.spi.transfer.security.SecurityException;

import java.security.PublicKey;

/** */
public class ClearTextPublicKeySerializer implements PublicKeySerializer {

  @Override
  public boolean canHandle(String scheme) {
    return "cleartext".equals(scheme);
  }

  @Override
  public String serialize(PublicKey publicKey) throws SecurityException {
    return BaseEncoding.base64().encode(publicKey.getEncoded());
  }
}
