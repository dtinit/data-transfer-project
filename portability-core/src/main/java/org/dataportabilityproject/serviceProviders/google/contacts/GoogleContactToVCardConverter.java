package org.dataportabilityproject.serviceProviders.google.contacts;

import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import ezvcard.VCard;
import ezvcard.property.Email;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleContactToVCardConverter {

  private static final Logger logger = LoggerFactory.getLogger(GoogleContactToVCardConverter.class);

  @VisibleForTesting
  static final int PRIMARY_PREF = 1;
  @VisibleForTesting
  static final int SECONDARY_PREF = 2;
  @VisibleForTesting
  static final String SOURCE_PARAM_NAME_ID = "Source_id";
  @VisibleForTesting
  static final String SOURCE_PARAM_NAME_TYPE = "Source_type";

  @VisibleForTesting
  static VCard convert(Person person) {
    VCard vCard = new VCard();

    /* Reluctant to set the VCard.Kind value, since a) there aren't that many type options for Google contacts,
    b) those type options are often wrong, and c) those type options aren't even reliably in the same place.
    Source: https://developers.google.com/people/api/rest/v1/people#personmetadata
    */

    Preconditions.checkArgument(atLeastOneNamePresent(person.getNames()),
        "At least one name must be present");
    convertToVCardNamesAndPopulate(vCard, person.getNames());

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

  private static Email convertToVCardEmail(EmailAddress personEmail) {
    // TODO(olsona): address Email.displayName
    // TODO(olsona): address Email.formattedType
    Email email = new Email(personEmail.getValue());
    email.setPref(getPref(personEmail.getMetadata()));

    return email;
  }

  private static void convertToVCardNamesAndPopulate(VCard vCard, List<Name> personNames) {
    // TODO(olsona): what if there's more than one primary name in a Google Contact?
    StructuredName primaryStructuredName = null;
    LinkedList<StructuredName> alternateStructuredNames = new LinkedList<>();
    for (Name personName : personNames) {
      StructuredName structuredName = convertToVCardNameSingle(personName);
      if (personName.getMetadata().getPrimary() != null && personName.getMetadata().getPrimary()) {
        // This is the (a?) primary name for the Person, so it should be the primary name in the
        // VCard.
        primaryStructuredName = structuredName;
      } else {
        alternateStructuredNames.add(structuredName);
      }
    }

    if (primaryStructuredName == null) {
      primaryStructuredName = alternateStructuredNames.pop();
    }

    vCard.addProperty(primaryStructuredName);
    vCard.addPropertyAlt(StructuredName.class, alternateStructuredNames);
  }

  private static StructuredName convertToVCardNameSingle(Name personName) {
    StructuredName structuredName = new StructuredName();
    structuredName.setFamily(personName.getFamilyName());
    structuredName.setGiven(personName.getGivenName());
    structuredName.setParameter(SOURCE_PARAM_NAME_ID, personName.getMetadata().getSource().getId());
    structuredName.setParameter(SOURCE_PARAM_NAME_TYPE, personName.getMetadata().getSource().getType());

    // TODO(olsona): address formatting, structure, phonetics, suffixes, prefixes
    return structuredName;
  }

  private static Telephone convertToVCardTelephone(PhoneNumber personNumber) {
    Telephone telephone = new Telephone(personNumber.getValue());
    telephone.setPref(getPref(personNumber.getMetadata()));
    return telephone;
  }

  private static int getPref(FieldMetadata metadata) {
    return metadata.getPrimary() ? PRIMARY_PREF : SECONDARY_PREF;
  }

  private static boolean atLeastOneNamePresent(List<Name> personNames) {
    return personNames.size() >= 1 && !personNames.get(0).isEmpty();
  }
}
