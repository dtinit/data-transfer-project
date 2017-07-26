package org.dataportabilityproject.dataModels.mail;

import com.google.common.base.MoreObjects;

// Model for a mail folder or label, which may contain sub folders and messages
public final class MailContainerModel {
  private final String id;
  private final String name;

  public MailContainerModel(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("name", name)
        .toString();
  }
}
