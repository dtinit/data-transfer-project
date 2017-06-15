package org.dataportabilityproject.webapp.auth;

/** Provides ids for users of data portability project. */
public interface IdProvider {
  /** Creates a new unique id. */
  String createId();
}
