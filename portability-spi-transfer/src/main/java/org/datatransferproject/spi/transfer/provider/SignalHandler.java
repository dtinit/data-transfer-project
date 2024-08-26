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

package org.datatransferproject.spi.transfer.provider;

import java.io.IOException;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.retry.RetryException;

/**
 * Signals the status of the Job to the corresponding Service.
 */
public interface SignalHandler<A extends AuthData> {

  /**
   * Sends the signal to thw provider typically over the Network. The default implementation logs the
   * signal info via {@link Monitor}. By default, the signal is turned ON via "transferSignalEnabled"
   * settings flag. Signalling can be explicitly turned off by setting "transferSignalEnabled" to
   * false in the Settings.
   *
   * @see YamlSettingsExtension - for turning off the Signalling feature.
   * @see JobProcessor - where the signals are triggered.
   * @see WorkerModule - feature flag (transferSignalEnabled) is wired.
   */
  default void sendSignal(SignalRequest signalRequest, AuthData authData, Monitor monitor)
    throws CopyExceptionWithFailureReason, IOException, RetryException {
    monitor.info(() -> "Default Signaller::" + signalRequest.toString());
  }
}
