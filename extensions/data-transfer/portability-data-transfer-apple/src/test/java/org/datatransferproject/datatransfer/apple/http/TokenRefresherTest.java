/*
 * Copyright 2024 The Data Transfer Project Authors.
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
package org.datatransferproject.datatransfer.apple.http;

import static org.datatransferproject.datatransfer.apple.http.FakeSynchronousSubscriber.assertPublishes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.http.HttpRequest;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.jupiter.api.Test;

public class TokenRefresherTest {
  @Test
  public void test_buildRefreshRequestUrlForAccessToken() {
    //
    // Arrange
    //
    final String fakeAuthUrl = "https://www.example.com/auth";
    TokensAndUrlAuthData fakeAuthData =
        new TokensAndUrlAuthData("my_access_token", "my_refresh_token", fakeAuthUrl);
    AppCredentials mockAppCredentials = mock(AppCredentials.class);
    when(mockAppCredentials.getKey()).thenReturn("fake-client_id-contents-here");
    when(mockAppCredentials.getSecret()).thenReturn("fake-client_secert-contents-here");

    //
    // Act
    //
    HttpRequest actualRequest =
        TokenRefresher.buildRefreshRequestUrlForAccessToken(fakeAuthData, mockAppCredentials)
            .build();

    //
    // Assert
    //
    assertEquals(actualRequest.method(), "POST");
    assertEquals(actualRequest.uri().toString(), fakeAuthUrl);
    assertFalse(actualRequest.bodyPublisher().isEmpty());
    assertPublishes(
        actualRequest.bodyPublisher().get(),
        "refresh_token=my_refresh_token&grant_type=refresh_token&client_secret=fake-client_secert-contents-here&client_id=fake-client_id-contents-here");
  }

  @Test
  public void test_buildRefreshRequestUrlForAccessToken_detectsIllegalStates() {
    assertTrue(false); /* DO NOT MERGE finish writing */
    //  assertThrows(
    //      IllegalStateException.class,
    //      () -> {
    //        return "";
    //      }); /* DO NOT MERGE*/
  }
}
