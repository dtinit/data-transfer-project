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

package org.datatransferproject.logging;

import com.google.common.base.Preconditions;
import java.util.UUID;
import org.apache.log4j.Layout;
import org.apache.log4j.helpers.ISO8601DateFormat;
import org.apache.log4j.spi.LoggingEvent;

public class EncryptingLayout extends Layout {

  static UUID jobId;

  public static void setJobId(UUID inputJobId) {
    Preconditions.checkState(jobId == null, "jobId has already been set");
    jobId = inputJobId;
  }

  @Override
  public String format(LoggingEvent event) {
    // TODO: read formatting from a config file
    return String.format("[%s] [%s]: %s - %s%s",
        new ISO8601DateFormat().format(event.timeStamp),
        jobId != null ? Long.toHexString(jobId.getMostSignificantBits()) : "undefined",
        event.getLevel().toString(),
        event.getRenderedMessage(), LINE_SEP);
  }

  @Override
  public boolean ignoresThrowable() {
    return true;
  }

  @Override
  public void activateOptions() {
  }
}
