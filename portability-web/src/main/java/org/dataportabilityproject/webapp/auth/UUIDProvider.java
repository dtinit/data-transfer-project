package org.dataportabilityproject.webapp.auth;

import java.util.UUID;

/** UUID based implementation of an {@link IdProvider}. */
public class UUIDProvider implements IdProvider {

  @Override
  public String createId() {
    return UUID.randomUUID().toString();
  }
}
