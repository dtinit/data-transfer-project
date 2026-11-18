/*
 * Copyright 2026 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.signals;

import java.util.function.BooleanSupplier;
import org.datatransferproject.spi.transfer.provider.SignalRequest;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

/** Sends one transfer telemetry signal to Google's receiver. */
interface TelemetrySender {

  /**
   * Sends {@code request}, authorized by the user's Google token in {@code authData}. A request
   * that fails with an error worth retrying is sent again, a limited number of times, but only
   * while {@code mayRetry} returns true. Logs a failure instead of throwing it. Can take a while,
   * since it retries.
   */
  void send(SignalRequest request, TokensAndUrlAuthData authData, BooleanSupplier mayRetry);
}
