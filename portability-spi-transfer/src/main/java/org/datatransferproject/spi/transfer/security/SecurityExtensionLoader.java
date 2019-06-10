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
package org.datatransferproject.spi.transfer.security;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ServiceLoader;
import org.datatransferproject.api.launcher.ExtensionContext;

/** Helper for loading the security extensions in a runtime. */
public class SecurityExtensionLoader {

  public static SecurityExtension getSecurityExtension(ExtensionContext context) {
    ImmutableList.Builder<SecurityExtension> builder = ImmutableList.builder();
    ServiceLoader.load(SecurityExtension.class)
        .iterator()
        .forEachRemaining(builder::add);
    ImmutableList<SecurityExtension> extensions = builder.build();
    Preconditions.checkState(
        extensions.size() == 1,
        "Exactly one CloudExtension is required, but found " + extensions.size());
    extensions.forEach(e -> e.initialize(context));
    return extensions.get(0);
  }
}
