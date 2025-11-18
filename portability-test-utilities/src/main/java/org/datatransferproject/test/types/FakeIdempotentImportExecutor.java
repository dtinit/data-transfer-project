package org.datatransferproject.test.types;

import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;

/** Unit-test friendly, fully-functional IdempotentImportExecutor, entirely in memory. */
// TODO migrate tests constructing InMemoryIdempotentImportExecutor to this class to avoid future
// noise where we have to touch every test (should InMemoryIdempotentImportExecutor start doing
// something unfriendly to tests).
public class FakeIdempotentImportExecutor extends InMemoryIdempotentImportExecutor {
  // TODO figure out how to get Gradle to let us mock(Monitor.class) ourselves here?
  private static final Monitor fakeMonitor = new Monitor() {};

  public FakeIdempotentImportExecutor() {
    super(FakeIdempotentImportExecutor.fakeMonitor);
  }
}
