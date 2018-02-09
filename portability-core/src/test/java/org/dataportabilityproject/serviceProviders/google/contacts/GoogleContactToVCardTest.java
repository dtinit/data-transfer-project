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

import com.google.api.services.people.v1.model.*;
import com.google.gdata.util.common.base.Pair;
import ezvcard.VCard;
import ezvcard.parameter.VCardParameters;
import ezvcard.property.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactToVCard.PRIMARY_PREF;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactToVCard.SECONDARY_PREF;

public class GoogleContactToVCardTest {
  private static final Logger logger = LoggerFactory.getLogger(GoogleContactToVCardTest.class);

  private static final FieldMetadata PRIMARY_FIELD_METADATA = new FieldMetadata().setPrimary(true);
  private static final FieldMetadata SECONDARY_FIELD_METADATA =
          new FieldMetadata().setPrimary(false);

  @Test
  public void testConversionToVCardEmail() {
    // Set up test: person with 1 primary email and 2 secondary emails
    String primaryString = "primary@email.com";
    String secondaryString1 = "secondary1@email.com";
    String secondaryString2 = "secondary2@email.com";
    EmailAddress primaryEmail = new EmailAddress().setValue(primaryString)
            .setMetadata(PRIMARY_FIELD_METADATA);
    EmailAddress secondaryEmail1 = new EmailAddress().setValue(secondaryString1)
            .setMetadata(SECONDARY_FIELD_METADATA);
    EmailAddress secondaryEmail2 = new EmailAddress().setValue(secondaryString2)
            .setMetadata(SECONDARY_FIELD_METADATA);
    Person person = new Person().setEmailAddresses(Arrays.asList(secondaryEmail1, primaryEmail,
            secondaryEmail2)); // Making sure order isn't a factor

    // Run test - NB, this Person only has emails
    VCard vCard = GoogleContactToVCard.convert(person);

    // Check results for correct values and preferences
    List<Email> resultPrimaryEmailList = getPropertiesWithPreference(vCard, Email.class,
            PRIMARY_PREF);
    assertThat(getValuesFromTextProperties(resultPrimaryEmailList)).containsExactly(primaryString);
    List<Email> resultSecondaryEmailList = getPropertiesWithPreference(vCard, Email.class,
            SECONDARY_PREF);
    assertThat(getValuesFromTextProperties(resultSecondaryEmailList))
            .containsExactly(secondaryString1, secondaryString2);
  }

  @Test
  public void testConversionToVCardNames() {
    // Set up Person with a primary name and a secondary name
    String primaryGivenName = "J. K.";
    String primaryFamilyName = "Rowling";
    Name primaryName = new Name().setGivenName(primaryGivenName)
            .setFamilyName(primaryFamilyName)
            .setMetadata(PRIMARY_FIELD_METADATA);

    String alternateGivenName1 = "Joanne";
    String alternateFamilyName1 = "Rowling";
    Name alternateName1 = new Name().setGivenName(alternateGivenName1)
            .setFamilyName(alternateFamilyName1)
            .setMetadata(SECONDARY_FIELD_METADATA);
    String alternateGivenName2 = "Robert";
    String alternateFamilyName2 = "Galbraith";
    Name alternateName2 = new Name().setGivenName(alternateGivenName2).setFamilyName(alternateFamilyName2)
            .setMetadata(SECONDARY_FIELD_METADATA);

    // Order shouldn't matter
    Person person = new Person().setNames(Arrays.asList(alternateName2, alternateName1, primaryName));

    // Run test
    VCard vCard = GoogleContactToVCard.convert(person);

    // Check name conversion correctness
    List<StructuredName> structuredNames = vCard.getStructuredNames();
    assertThat(structuredNames.size()).isEqualTo(3);

    // Check primary (non-alternate) names
    List<StructuredName> actualPrimaryNames = structuredNames.stream().filter(n -> n.getAltId() == null).collect
            (Collectors.toList());
    assertThat(actualPrimaryNames.size()).isEqualTo(1);
    assertThat(getValuesFromProperties(actualPrimaryNames, StructuredName::getGiven)).containsExactly(primaryGivenName);
    assertThat(getValuesFromProperties(actualPrimaryNames, StructuredName::getFamily)).containsExactly
            (primaryFamilyName);

    // Check alternate names
    List<StructuredName> actualAlternateNames = structuredNames.stream().filter(n -> n.getAltId() != null)
            .collect(Collectors.toList());
    assertThat(actualAlternateNames.size()).isEqualTo(2);
    List<Pair<String, String>> firstAndLastNames = actualAlternateNames.stream().map(a -> Pair.of(a.getGiven(), a
            .getFamily())).collect(Collectors.toList());
    assertThat(firstAndLastNames).containsExactly(
            Pair.of(alternateGivenName1, alternateFamilyName1),
            Pair.of(alternateGivenName2, alternateFamilyName2));
  }

  @Test
  public void testConversionToVCardTelephone() {
    // Set up test: person with 2 primary phone numbers and 1 secondary phone number
    String primaryValue1 = "334-844-4244";
    String primaryValue2 = "411";
    String secondaryValue = "(555) 867-5309";
    PhoneNumber primaryPhone1 = new PhoneNumber().setValue(primaryValue1)
            .setMetadata(PRIMARY_FIELD_METADATA);
    PhoneNumber primaryPhone2 = new PhoneNumber().setValue(primaryValue2)
            .setMetadata(PRIMARY_FIELD_METADATA);
    PhoneNumber secondaryPhone = new PhoneNumber().setValue(secondaryValue)
            .setMetadata(SECONDARY_FIELD_METADATA);
    Person person = new Person()
            .setPhoneNumbers(Arrays.asList(secondaryPhone, primaryPhone1, primaryPhone2));

    // Run test
    VCard vCard = GoogleContactToVCard.convert(person);

    // Check results for correct values and preferences
    List<Telephone> resultPrimaryPhoneList = getPropertiesWithPreference(vCard, Telephone.class,
            PRIMARY_PREF);
    assertThat(getValuesFromProperties(resultPrimaryPhoneList, Telephone::getText))
            .containsExactly(primaryValue1, primaryValue2);
    List<Telephone> resultSecondaryPhoneList = getPropertiesWithPreference(vCard, Telephone.class,
            SECONDARY_PREF);
    assertThat(getValuesFromProperties(resultSecondaryPhoneList, Telephone::getText))
            .containsExactly(secondaryValue);
  }

  private static <T extends VCardProperty, V> List<V> getValuesFromProperties(List<T> propertyList,
                                                                              Function<T, V> function) {
    return propertyList.stream().map(function).collect(Collectors.toList());
  }

  private static <T extends TextProperty> List<String> getValuesFromTextProperties(
          List<T> propertyList) {
    return getValuesFromProperties(propertyList, T::getValue);
  }

  private static <T extends VCardProperty> List<T> getPropertiesWithPreference(VCard vCard,
                                                                               Class<T> clazz, int preference) {
    return vCard.getProperties(clazz).stream()
            .filter(p -> p.getParameter(VCardParameters.PREF).equals(Integer.toString(preference)))
            .collect(Collectors.toList());
  }
}
