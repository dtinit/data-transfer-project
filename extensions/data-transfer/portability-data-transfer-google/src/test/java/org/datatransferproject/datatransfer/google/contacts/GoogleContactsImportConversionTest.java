/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.datatransfer.google.contacts;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.CONTACT_SOURCE_TYPE;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.SOURCE_PARAM_NAME_TYPE;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.VCARD_PRIMARY_PREF;

import com.google.api.services.people.v1.model.Address;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import com.google.gdata.util.common.base.Nullable;
import com.google.gdata.util.common.base.Pair;
import ezvcard.VCard;
import ezvcard.property.Email;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleContactsImportConversionTest {
  private VCard defaultVCard;

  private static Pair<String, String> getGivenAndFamilyValues(Name name) {
    return Pair.of(name.getGivenName(), name.getFamilyName());
  }

  private static StructuredName makeStructuredName(
      String givenName, String familyName, @Nullable String sourceType) {
    StructuredName structuredName = new StructuredName();
    structuredName.setGiven(givenName);
    structuredName.setFamily(familyName);
    if (sourceType != null) {
      structuredName.setParameter(SOURCE_PARAM_NAME_TYPE, sourceType);
    }
    return structuredName;
  }

  private static <F, V> List<V> getValuesFromFields(List<F> fields, Function<F, V> function) {
    return fields.stream().map(function).collect(Collectors.toList());
  }

  @BeforeEach
  public void setup() {
    defaultVCard = new VCard();
    defaultVCard.setStructuredName(makeStructuredName("Haskell", "Curry", CONTACT_SOURCE_TYPE));
  }

  @Test
  public void testConversionToGoogleNames() {
    // Set up vCard with a primary name and one secondary name
    String primaryGivenName = "Mark";
    String primaryFamilyName = "Twain";
    String primarySourceType = "CONTACT";
    StructuredName primaryName =
        makeStructuredName(primaryGivenName, primaryFamilyName, primarySourceType);

    String altGivenName = "Samuel";
    String altFamilyName = "Clemens";
    String altSourceType = "PROFILE";
    StructuredName altName = makeStructuredName(altGivenName, altFamilyName, altSourceType);

    VCard vCard = new VCard();
    vCard.addProperty(primaryName);
    vCard.addPropertyAlt(StructuredName.class, Collections.singleton(altName));

    // Run test
    Person person = GoogleContactsImporter.convert(vCard);

    // Check results
    // Correct number of names
    assertThat(person.getNames().size()).isEqualTo(1);

    // Check primary names
    List<Name> actualPrimaryNames =
        person
            .getNames()
            .stream()
            .filter(a -> a.getMetadata().getPrimary())
            .collect(Collectors.toList());
    List<Pair<String, String>> actualPrimaryNameValues =
        actualPrimaryNames
            .stream()
            .map(GoogleContactsImportConversionTest::getGivenAndFamilyValues)
            .collect(Collectors.toList());
    assertThat(actualPrimaryNameValues)
        .containsExactly(Pair.of(primaryGivenName, primaryFamilyName));
    List<String> actualPrimaryNameSourceValues =
        actualPrimaryNames
            .stream()
            .map(a -> a.getMetadata().getSource().getType())
            .collect(Collectors.toList());
    assertThat(actualPrimaryNameSourceValues).containsExactly(primarySourceType);

    // Check secondary names - there shouldn't be any
    List<Name> actualSecondaryNames =
        person
            .getNames()
            .stream()
            .filter(a -> !a.getMetadata().getPrimary())
            .collect(Collectors.toList());
    assertThat(actualSecondaryNames).isEmpty();
  }

  @Test
  public void testConversionToGoogleAddresses() {
    // Set up vCard with a primary address and a secondary address
    String primaryStreet = "221B Baker St";
    String primaryLocality = "London";
    ezvcard.property.Address primaryAddress = new ezvcard.property.Address();
    primaryAddress.setStreetAddress(primaryStreet);
    primaryAddress.setLocality(primaryLocality);
    primaryAddress.setPref(VCARD_PRIMARY_PREF);

    String altStreet = "42 Wallaby Way";
    String altLocality = "Sydney";
    ezvcard.property.Address altAddress = new ezvcard.property.Address();
    altAddress.setStreetAddress(altStreet);
    altAddress.setLocality(altLocality);
    altAddress.setPref(VCARD_PRIMARY_PREF + 1);

    // Add addresses to vCard.  Order shouldn't matter.
    VCard vCard = defaultVCard;
    vCard.addAddress(primaryAddress);
    vCard.addAddress(altAddress);

    // Run test
    Person person = GoogleContactsImporter.convert(vCard);

    // Check results
    // Check correct number of addresses
    assertThat(person.getAddresses().size()).isEqualTo(2);

    // Check primary address
    List<Address> actualPrimaryAddresses =
        person
            .getAddresses()
            .stream()
            .filter(a -> a.getMetadata().getPrimary())
            .collect(Collectors.toList());
    List<String> actualPrimaryAddressStreets =
        getValuesFromFields(actualPrimaryAddresses, Address::getStreetAddress);
    assertThat(actualPrimaryAddressStreets).containsExactly(primaryStreet);

    // Check secondary address
    List<Address> actualSecondaryAddresses =
        person
            .getAddresses()
            .stream()
            .filter(a -> !a.getMetadata().getPrimary())
            .collect(Collectors.toList());
    List<String> actualSecondaryAddressStreets =
        getValuesFromFields(actualSecondaryAddresses, Address::getStreetAddress);
    assertThat(actualSecondaryAddressStreets).containsExactly(altStreet);
  }

  @Test
  public void testConversionToGooglePhones() {
    // Set up test: vCard with 2 primary phone numbers and 1 secondary phone number
    String primaryValue1 = "334-844-4244";
    String primaryValue2 = "411";
    String secondaryValue = "(555) 867-5309";
    Telephone primaryTelephone1 = new Telephone(primaryValue1);
    primaryTelephone1.setPref(VCARD_PRIMARY_PREF);
    Telephone primaryTelephone2 = new Telephone(primaryValue2);
    primaryTelephone2.setPref(VCARD_PRIMARY_PREF);
    Telephone secondaryTelephone = new Telephone(secondaryValue);
    secondaryTelephone.setPref(VCARD_PRIMARY_PREF + 1);

    // Add numbers to vCard.  Order shouldn't matter.
    VCard vCard = defaultVCard;
    vCard.addTelephoneNumber(secondaryTelephone);
    vCard.addTelephoneNumber(primaryTelephone1);
    vCard.addTelephoneNumber(primaryTelephone2);

    // Run test
    Person person = GoogleContactsImporter.convert(vCard);

    // Check results
    // Correct number of phone numbers
    assertThat(person.getPhoneNumbers().size()).isEqualTo(3);

    // Check primary phone numbers
    List<PhoneNumber> actualPrimaryNumbers =
        person
            .getPhoneNumbers()
            .stream()
            .filter(a -> a.getMetadata().getPrimary())
            .collect(Collectors.toList());
    List<String> actualPrimaryNumberStrings =
        getValuesFromFields(actualPrimaryNumbers, PhoneNumber::getValue);
    assertThat(actualPrimaryNumberStrings).containsExactly(primaryValue1, primaryValue2);

    // Check secondary phone numbers
    List<PhoneNumber> actualSecondaryNumbers =
        person
            .getPhoneNumbers()
            .stream()
            .filter(a -> !a.getMetadata().getPrimary())
            .collect(Collectors.toList());
    List<String> actualSecondaryNumberStrings =
        getValuesFromFields(actualSecondaryNumbers, PhoneNumber::getValue);
    assertThat(actualSecondaryNumberStrings).containsExactly(secondaryValue);
  }

  @Test
  public void testConversionToGoogleEmails() {
    // Set up test: person with 1 primary email and 2 secondary emails
    String primaryString = "primary@email.com";
    String secondaryString1 = "secondary1@email.com";
    String secondaryString2 = "secondary2@email.com";
    Email primaryEmail = new Email(primaryString);
    primaryEmail.setPref(VCARD_PRIMARY_PREF);
    Email secondaryEmail1 = new Email(secondaryString1);
    secondaryEmail1.setPref(VCARD_PRIMARY_PREF + 1);
    Email secondaryEmail2 = new Email(secondaryString2);
    secondaryEmail2.setPref(VCARD_PRIMARY_PREF + 1);

    // Add emails to vCard.  Order shouldn't matter.
    VCard vCard = defaultVCard;
    vCard.addEmail(secondaryEmail1);
    vCard.addEmail(primaryEmail);
    vCard.addEmail(secondaryEmail2);

    // Run test
    Person person = GoogleContactsImporter.convert(vCard);

    // Check results
    // Correct number of emails
    assertThat(person.getEmailAddresses().size()).isEqualTo(3);

    // Check primary email addresses
    List<EmailAddress> actualPrimaryEmails =
        person
            .getEmailAddresses()
            .stream()
            .filter(a -> a.getMetadata().getPrimary())
            .collect(Collectors.toList());
    List<String> actualPrimaryEmailsStrings =
        getValuesFromFields(actualPrimaryEmails, EmailAddress::getValue);
    assertThat(actualPrimaryEmailsStrings).containsExactly(primaryString);

    // Check secondary email addresses
    List<EmailAddress> actualSecondaryEmails =
        person
            .getEmailAddresses()
            .stream()
            .filter(a -> !a.getMetadata().getPrimary())
            .collect(Collectors.toList());
    List<String> actualSecondaryEmailsStrings =
        getValuesFromFields(actualSecondaryEmails, EmailAddress::getValue);
    assertThat(actualSecondaryEmailsStrings).containsExactly(secondaryString1, secondaryString2);
  }
}
