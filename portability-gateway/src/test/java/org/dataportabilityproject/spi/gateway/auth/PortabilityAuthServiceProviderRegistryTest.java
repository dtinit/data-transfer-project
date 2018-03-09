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

package org.dataportabilityproject.spi.gateway.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.dataportabilityproject.gateway.PortabilityAuthServiceProviderRegistry;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PortabilityAuthServiceProviderRegistryTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void requireImportAndExportTest() {
    List<String> supportedImportTypes = ImmutableList.of("photos", "contacts");
    List<String> supportedExportTypes = ImmutableList.of("contacts");

    AuthServiceProvider mockAuthProvider = mock(AuthServiceProvider.class);
    when(mockAuthProvider.getExportTypes()).thenReturn(supportedExportTypes);
    when(mockAuthProvider.getImportTypes()).thenReturn(supportedImportTypes);
    when(mockAuthProvider.getServiceId()).thenReturn("mockAuthProvider");
    thrown.expect(IllegalArgumentException.class);
    thrown.expectMessage("available for import but not export");

    AuthServiceProviderRegistry registry =
        new PortabilityAuthServiceProviderRegistry(
            ImmutableMap.of("mockServiceProvider", mockAuthProvider));
  }
}
