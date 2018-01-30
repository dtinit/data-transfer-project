package org.dataportabilityproject.types.transfer.models.contacts;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.dataportabilityproject.types.transfer.models.DataModel;

/**
 * A collection of contacts as serialized vCards.
 */
public class ContactsModelWrapper extends DataModel {
    private String vCards;

    @JsonCreator
    public ContactsModelWrapper(@JsonProperty("vCards") String vCards) {
        this.vCards = vCards;
    }

    public String getVCards() {
        return vCards;
    }
}
