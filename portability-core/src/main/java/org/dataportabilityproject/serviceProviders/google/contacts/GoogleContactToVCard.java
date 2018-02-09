package org.dataportabilityproject.serviceProviders.google.contacts;

import com.google.api.services.people.v1.model.*;
import com.google.common.annotations.VisibleForTesting;
import ezvcard.VCard;
import ezvcard.property.Email;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;

import java.util.LinkedList;
import java.util.List;

public class GoogleContactToVCard {

  @VisibleForTesting
  static final int PRIMARY_PREF = 1;
  @VisibleForTesting
  static final int SECONDARY_PREF = 2;

  @VisibleForTesting
  static VCard convert(Person person) {
    VCard vCard = new VCard();

    /* Reluctant to set the VCard.Kind value, since a) there aren't that many type options for Google contacts,
    b) those type options are often wrong, and c) those type options aren't even reliably in the same place.
    Source: https://developers.google.com/people/api/rest/v1/people#personmetadata
    */

    if (person.getNames() != null) {
      convertToVCardNamesAndPopulate(vCard, person.getNames());
    }

    if (person.getPhoneNumbers() != null) {
      // VCard API does not support adding multiple telephone numbers at once
      person.getPhoneNumbers().forEach(n -> vCard.addTelephoneNumber(convertToVCardTelephone(n)));
    }

    if (person.getEmailAddresses() != null) {
      // VCard API does not support adding multiple emails at once
      person.getEmailAddresses().forEach(e -> vCard.addEmail(convertToVCardEmail(e)));
    }

    return vCard;
  }

  private static int getPref(FieldMetadata metadata) {
    return metadata.getPrimary() ? PRIMARY_PREF : SECONDARY_PREF;
  }

  private static Email convertToVCardEmail(EmailAddress personEmail) {
    // TODO(olsona): address Email.displayName
    // TODO(olsona): address Email.formattedType
    Email email = new Email(personEmail.getValue());
    email.setPref(getPref(personEmail.getMetadata()));

    return email;
  }

  private static void convertToVCardNamesAndPopulate(VCard vCard, List<Name> personNames) {
    // TODO(olsona): what if there's more than one primary name in a Google Contact?
    LinkedList<StructuredName> alternateVCardNames = new LinkedList<>();
    for (Name personName : personNames) {
      if (personName.getMetadata().getPrimary()) {
        // This is the primary name for the Person, so it should be the primary name in the VCard.
        vCard.setStructuredName(convertToVCardNameSingle(personName));
      } else {
        alternateVCardNames.add(convertToVCardNameSingle(personName));
      }
    }
    if (vCard.getStructuredName() == null) {
      // No personName was set as primary, so we'll just get the first alternate name
      vCard.setStructuredName(alternateVCardNames.pop());
    }

    vCard.setStructuredNameAlt(alternateVCardNames.toArray(new StructuredName[alternateVCardNames.size()]));
  }

  private static StructuredName convertToVCardNameSingle(Name personName) {
    StructuredName structuredName = new StructuredName();
    structuredName.setFamily(personName.getFamilyName());
    structuredName.setGiven(personName.getGivenName());
    structuredName.getPrefixes().add(personName.getHonorificPrefix());
    structuredName.getSuffixes().add(personName.getHonorificSuffix());

    // TODO(olsona): address formatting, structure, phonetics
    return structuredName;
  }

  private static Telephone convertToVCardTelephone(PhoneNumber personNumber) {
    Telephone telephone = new Telephone(personNumber.getValue());
    telephone.setPref(getPref(personNumber.getMetadata()));
    return telephone;
  }
}
