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
import com.google.api.services.people.v1.model.PhoneNumber;
import ezvcard.VCard;
import ezvcard.parameter.VCardParameters;
import ezvcard.property.Email;
import ezvcard.property.Telephone;
import ezvcard.property.TextProperty;
import ezvcard.property.VCardProperty;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactToVCard.PRIMARY_PREF;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactToVCard.SECONDARY_PREF;

public class GoogleContactToVCardTest {
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
