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

package org.datatransferproject.spi.transfer.types.signals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.EndReason;
import org.datatransferproject.spi.transfer.types.signals.JobLifeCycle.State;
import org.junit.jupiter.api.Test;

public class JobLifeCycleTest {

  @Test
  public void inProgress_hasInProgressStateAndNoEndReason() {
    JobLifeCycle inProgress = JobLifeCycle.IN_PROGRESS();

    assertEquals(State.IN_PROGRESS, inProgress.state());
    assertNull(inProgress.endReason());
    assertNull(inProgress.failureReason());
  }

  @Test
  public void userCancelled_isAValidEndReason() {
    JobLifeCycle cancelled =
        JobLifeCycle.builder().setState(State.ENDED).setEndReason(EndReason.USER_CANCELLED).build();

    assertEquals(EndReason.USER_CANCELLED, cancelled.endReason());
  }

  @Test
  public void existingEndReasons_keepTheirOrdinals() {
    // USER_CANCELLED is appended, so anything persisting ordinals is unaffected.
    assertEquals(0, EndReason.PAUSED.ordinal());
    assertEquals(1, EndReason.INTERRUPTED.ordinal());
    assertEquals(2, EndReason.SUCCESSFULLY_COMPLETED.ordinal());
    assertEquals(3, EndReason.PARTIALLY_COMPLETED.ordinal());
    assertEquals(4, EndReason.ERRORED.ordinal());
  }
}
