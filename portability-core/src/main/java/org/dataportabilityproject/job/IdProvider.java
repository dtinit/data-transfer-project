package org.dataportabilityproject.job;

/** Provides ids for users of data portability project. */
public interface IdProvider {
  /** Creates a new unique id. */
  String createId();
}
