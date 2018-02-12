/*
 * Copyright 2018 The Data-Portability Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dataportabilityproject.serviceProviders.google.contacts;

import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.Person;
import com.google.common.annotations.VisibleForTesting;
import ezvcard.VCard;
import ezvcard.property.Email;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import ezvcard.VCard;
import ezvcard.property.Email;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VCardToGoogleContactConverter {

  private static final Logger logger = LoggerFactory.getLogger(VCardToGoogleContactConverter.class);

  @VisibleForTesting
  static final int PRIMARY_PREF = 1;
  @VisibleForTesting
  static final int SECONDARY_PREF = 2;

  static final FieldMetadata PRIMARY_FIELD_METADATA = new FieldMetadata().setPrimary(true);
  static final FieldMetadata SECONDARY_FIELD_METADATA = new FieldMetadata().setPrimary(false);

  @VisibleForTesting
  static Person convert(VCard vCard) {
    Person person = new Person();

    Preconditions.checkArgument(atLeastOneNamePresent(vCard), "At least one name must be present");
    person.setNames(vCard.getStructuredNames().stream()
            .map(VCardToGoogleContactConverter::convertToGoogleName)
            .collect(Collectors.toList()));

    if (vCard.getTelephoneNumbers() != null) {
      person.setPhoneNumbers(vCard.getTelephoneNumbers().stream()
          .map(VCardToGoogleContactConverter::convertToGooglePhoneNumber)
          .collect(Collectors.toList()));
    }

    if (vCard.getEmails() != null) {
      person.setEmailAddresses(vCard.getEmails().stream()
          .map(VCardToGoogleContactConverter::convertToGoogleEmail)
          .collect(Collectors.toList()));
    }

    return person;
  }

  private static Name convertToGoogleName(StructuredName vCardName) {
    Name name = new Name();
    name.setFamilyName(vCardName.getFamily());
    name.setGivenName(vCardName.getGiven());
    if (vCardName.getAltId() == null) {
      name.setMetadata(PRIMARY_FIELD_METADATA);
    } else {
      name.setMetadata(SECONDARY_FIELD_METADATA);
    }
    // TODO(olsona): address formatting, structure, phonetics, suffixes, prefixes

    return name;
  }

  private static PhoneNumber convertToGooglePhoneNumber(Telephone vCardTelephone) {
    PhoneNumber phoneNumber = new PhoneNumber();
    phoneNumber.setValue(vCardTelephone.getText());
    if (vCardTelephone.getPref() == PRIMARY_PREF) {
      phoneNumber.setMetadata(PRIMARY_FIELD_METADATA);
    } else {
      phoneNumber.setMetadata(SECONDARY_FIELD_METADATA);
    }

    return phoneNumber;
  }

  private static EmailAddress convertToGoogleEmail(Email vCardEmail) {
    EmailAddress emailAddress = new EmailAddress();
    emailAddress.setValue(vCardEmail.getValue());
    if (vCardEmail.getPref() == PRIMARY_PREF) {
      emailAddress.setMetadata(PRIMARY_FIELD_METADATA);
    } else {
      emailAddress.setMetadata(SECONDARY_FIELD_METADATA);
    }
    // TODO(olsona): address display name, formatted type, etc

    return emailAddress;
  }

  private static boolean atLeastOneNamePresent(VCard vCard) {
    // TODO(olsona): there are more checks we could make
    return vCard.getStructuredNames().size() >= 1 && vCard.getStructuredName() != null;
  }
}
