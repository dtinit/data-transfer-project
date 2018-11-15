package org.datatransferproject.types.common.models.contacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.datatransferproject.types.common.models.DataModel;

/** A collection of contacts as serialized vCards. */
public class ContactsModelWrapper extends DataModel {
  private final String vCards;

  @JsonCreator
  public ContactsModelWrapper(@JsonProperty("vCards") String vCards) {
    this.vCards = vCards;
  }

  public String getVCards() {
    return vCards;
  }
}
