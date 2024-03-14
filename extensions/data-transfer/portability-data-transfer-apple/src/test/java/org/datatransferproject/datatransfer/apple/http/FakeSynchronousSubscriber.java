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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;

/**
 * Simplifies JDK 11 APIs that don't provide testing utilities to drain a {@link Flow.Publisher} for
 * easy test-assertions.
 *
 * <p>Designed to be used solely via {@link FakeSynchronousSubscriber#assertPublishes}.
 */
// TODO(jzacsh, hgandhi90) adapted from popular https://stackoverflow.com/a/59347350 post but we
// should consider adding a proper gradle dependency for https://github.com/PGSSoft/HttpClientMock
// so we can delete all of this; in the meantime we have this hand-written thing to enable easier
// assertion logic above where java.util.concurrent.Flow.Publisher interface is involved.
public class FakeSynchronousSubscriber implements Flow.Subscriber<ByteBuffer> {
  /* Subscribe to, and synchronously drain the contents of a publisher. */
  public static byte[] syncDrain(HttpRequest.BodyPublisher actualPublisher) {
    FakeSynchronousSubscriber flowSubscriber = new FakeSynchronousSubscriber();
    actualPublisher.subscribe(flowSubscriber);
    return flowSubscriber.getBodyItems().get(0).array();
  }

  public static void assertPublishes(HttpRequest.BodyPublisher actualPublisher, String utf8String) {
    assertPublishes(actualPublisher, ByteBuffer.wrap(utf8String.getBytes(UTF_8)));
  }

  public static void assertPublishes(
      HttpRequest.BodyPublisher actualPublisher, ByteBuffer expectedPublication) {
    byte[] actual = syncDrain(actualPublisher);
    byte[] expected = expectedPublication.array();
    assertArrayEquals(actual, expected, debugStringEqualityAssertionFailure(actual, expected));
  }

  private static String debugStringEqualityAssertionFailure(byte[] actual, byte[] expected) {
    final String pythonStyleMultilineHorizontalRule = "\"\"\"";
    return String.format(
        "synchronously-drained publisher (len=%d) has different contents than expected"
            + " publication (len=%d); force-stringifying both for inspection:\n"
            + "ACTUAL byte buffer (printed as if UTF8 string):\n%s\n%s\n%s\n"
            + "EXPECTED byte buffer (printed as if UTF8 string):\n%s\n%s\n%s\n",
        actual.length,
        expected.length,

        // expected block
        pythonStyleMultilineHorizontalRule,
        new String(actual, UTF_8),
        pythonStyleMultilineHorizontalRule,

        // expected block
        pythonStyleMultilineHorizontalRule,
        new String(expected, UTF_8),
        pythonStyleMultilineHorizontalRule);
  }

  private final CountDownLatch latch = new CountDownLatch(1);
  private List<ByteBuffer> bodyItems = new ArrayList<>();

  public List<ByteBuffer> getBodyItems() {
    try {
      latch.await();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    return bodyItems;
  }

  @Override
  public void onSubscribe(Flow.Subscription subscription) {
    // Retrieve all parts
    subscription.request(Long.MAX_VALUE);
  }

  @Override
  public void onNext(ByteBuffer item) {
    bodyItems.add(item);
  }

  @Override
  public void onError(Throwable throwable) {
    latch.countDown();
  }

  @Override
  public void onComplete() {
    latch.countDown();
  }
}
