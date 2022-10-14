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

package org.datatransferproject.api.auth;

import static org.datatransferproject.types.common.models.DataVertical.CONTACTS;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.datatransferproject.spi.api.auth.AuthDataGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.types.common.models.DataVertical;

import org.junit.jupiter.api.Test;

public class PortabilityAuthServiceExtensionRegistryTest {

  private AuthServiceExtension getMockedAuthProvider(
      List<DataVertical> supportedImportTypes, List<DataVertical> supportedExportTypes, String serviceId) {
    AuthServiceExtension mockAuthProvider = mock(AuthServiceExtension.class);

    when(mockAuthProvider.getExportTypes()).thenReturn(supportedExportTypes);
    when(mockAuthProvider.getImportTypes()).thenReturn(supportedImportTypes);
    when(mockAuthProvider.getServiceId()).thenReturn(serviceId);

    return mockAuthProvider;
  }

  @Test
  public void requireImportAndExportTest() {
    List<DataVertical> supportedImportTypes = ImmutableList.of(PHOTOS, CONTACTS);
    List<DataVertical> supportedExportTypes = ImmutableList.of(CONTACTS);

    AuthServiceExtension mockAuthProvider = getMockedAuthProvider(
        supportedImportTypes, supportedExportTypes, "mockAuthProvider");

    Throwable throwable = assertThrows(IllegalArgumentException.class, () -> {
      new PortabilityAuthServiceProviderRegistry(
          ImmutableMap.of("mockServiceProvider", mockAuthProvider));
    });
    assertTrue(throwable.getMessage().contains("available for import but not export"));
  }

  @Test
  public void testGetTransferDataTypes() {
    List<DataVertical> supportedServiceTypes = ImmutableList.of(PHOTOS, CONTACTS);

    AuthServiceExtension mockAuthProvider = getMockedAuthProvider(
        supportedServiceTypes, supportedServiceTypes, "mockAuthProvider");

    AuthServiceProviderRegistry registry =
        new PortabilityAuthServiceProviderRegistry(
            ImmutableMap.of("mockServiceProvider", mockAuthProvider));

    Set<DataVertical> actual = registry.getTransferDataTypes();

    final DataVertical[] services = new DataVertical[] {PHOTOS, CONTACTS};
    Set<DataVertical> expected = new HashSet<>(Arrays.asList(services));

    assertEquals(actual, expected);
  }

  @Test
  public void testGetExportServices1() {
    List<DataVertical> supportedServiceTypes1 = ImmutableList.of(PHOTOS, CONTACTS);
    List<DataVertical> supportedServiceTypes2 = ImmutableList.of(CONTACTS);

    AuthServiceExtension mockAuthProvider1 = getMockedAuthProvider(
        supportedServiceTypes1, supportedServiceTypes1, "mockAuthProvider1");
    AuthServiceExtension mockAuthProvider2 = getMockedAuthProvider(
        supportedServiceTypes2, supportedServiceTypes2, "mockAuthProvider2");

    AuthServiceProviderRegistry registry =
        new PortabilityAuthServiceProviderRegistry(
            ImmutableMap.of("mockServiceProvider1", mockAuthProvider1,
                "mockServiceProvider2", mockAuthProvider2));

    Set<String> actual = registry.getExportServices(CONTACTS);
    final String[] services = new String[]{"mockAuthProvider1", "mockAuthProvider2"};
    Set<String> expected = new HashSet<>(Arrays.asList(services));

    assertEquals(actual, expected);
  }

  @Test
  public void testGetExportServices2() {
    List<DataVertical> supportedServiceTypes1 = ImmutableList.of(PHOTOS, CONTACTS);
    List<DataVertical> supportedServiceTypes2 = ImmutableList.of(CONTACTS);

    AuthServiceExtension mockAuthProvider1 = getMockedAuthProvider(
        supportedServiceTypes1, supportedServiceTypes1, "mockAuthProvider1");
    AuthServiceExtension mockAuthProvider2 = getMockedAuthProvider(
        supportedServiceTypes2, supportedServiceTypes2, "mockAuthProvider2");

    AuthServiceProviderRegistry registry =
        new PortabilityAuthServiceProviderRegistry(
            ImmutableMap.of("mockServiceProvider1", mockAuthProvider1,
                "mockServiceProvider2", mockAuthProvider2));

    Set<String> actual = registry.getExportServices(PHOTOS);
    final String[] services = new String[] {"mockAuthProvider1"};
    Set<String> expected = new HashSet<>(Arrays.asList(services));

    assertEquals(actual, expected);
  }

  @Test
  public void testGetImportServices1() {
    List<DataVertical> supportedServiceTypes1 = ImmutableList.of(PHOTOS, CONTACTS);
    List<DataVertical> supportedServiceTypes2 = ImmutableList.of(CONTACTS);

    AuthServiceExtension mockAuthProvider1 = getMockedAuthProvider(
        supportedServiceTypes1, supportedServiceTypes1, "mockAuthProvider1");
    AuthServiceExtension mockAuthProvider2 = getMockedAuthProvider(
        supportedServiceTypes2, supportedServiceTypes2, "mockAuthProvider2");
    AuthServiceProviderRegistry registry =
        new PortabilityAuthServiceProviderRegistry(
            ImmutableMap.of("mockServiceProvider1", mockAuthProvider1,
                "mockServiceProvider2", mockAuthProvider2));

    Set<String> actual = registry.getImportServices(CONTACTS);
    final String[] services = new String[]{"mockAuthProvider1", "mockAuthProvider2"};
    Set<String> expected = new HashSet<>(Arrays.asList(services));

    assertEquals(actual, expected);
  }

  @Test
  public void testGetImportServices2() {
    List<DataVertical> supportedServiceTypes1 = ImmutableList.of(PHOTOS, CONTACTS);
    List<DataVertical> supportedServiceTypes2 = ImmutableList.of(CONTACTS);

    AuthServiceExtension mockAuthProvider1 = getMockedAuthProvider(
        supportedServiceTypes1, supportedServiceTypes1, "mockAuthProvider1");
    AuthServiceExtension mockAuthProvider2 = getMockedAuthProvider(
        supportedServiceTypes2, supportedServiceTypes2, "mockAuthProvider2");

    AuthServiceProviderRegistry registry =
        new PortabilityAuthServiceProviderRegistry(
            ImmutableMap.of("mockServiceProvider1", mockAuthProvider1,
                "mockServiceProvider2", mockAuthProvider2));

    Set<String> actual = registry.getImportServices(PHOTOS);
    final String[] services = new String[] {"mockAuthProvider1"};
    Set<String> expected = new HashSet<>(Arrays.asList(services));

    assertEquals(actual, expected);
  }

  @Test
  public void testGetAuthDataGenerator() {
    List<DataVertical> supportedServiceTypes = ImmutableList.of(PHOTOS, CONTACTS);
    AuthServiceExtension mockAuthProvider = getMockedAuthProvider(
        supportedServiceTypes, supportedServiceTypes, "mockAuthProvider");

    when(mockAuthProvider
        .getAuthDataGenerator(CONTACTS, AuthServiceProviderRegistry.AuthMode.EXPORT))
        .thenReturn(mock(AuthDataGenerator.class));

    AuthServiceProviderRegistry registry =
        new PortabilityAuthServiceProviderRegistry(
            ImmutableMap.of("mockServiceProvider", mockAuthProvider));

    AuthDataGenerator actual = registry.getAuthDataGenerator(
        "mockServiceProvider", CONTACTS, AuthServiceProviderRegistry.AuthMode.EXPORT);

    assertNotNull(actual);
  }
}
