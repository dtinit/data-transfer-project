package org.datatransferproject.api.launcher;

import java.util.function.Supplier;

/**
 * The system monitoring and logging interface which can be obtained either via injection or from
 * {@link ExtensionContext#getService(Class)}.
 *
 * <p>Implementations are responsible for processing and forwarding events to an external sink or
 * service.
 */
public interface Monitor {

  /**
   * Records a severe (error) level event.
   *
   * @param supplier the event message
   * @param data optional data items
   */
  default void severe(Supplier<String> supplier, Object... data) {}

  /**
   * Records a info level event.
   *
   * @param supplier the event message
   * @param data optional data items
   */
  default void info(Supplier<String> supplier, Object... data) {}

  /**
   * Records a debug level event.
   *
   * @param supplier the event message
   * @param data optional data items
   */
  default void debug(Supplier<String> supplier, Object... data) {}
}
