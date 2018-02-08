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

import static com.google.common.truth.Truth.assertThat;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactsService
    .PERSON_FIELDS;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactsService
    .PRIMARY_PREF;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactsService
    .SECONDARY_PREF;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactsService
    .SELF_RESOURCE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleService.People.Connections;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.GetPeopleResponse;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PersonResponse;
import com.google.api.services.people.v1.model.PhoneNumber;
import ezvcard.VCard;
import ezvcard.parameter.VCardParameters;
import ezvcard.property.Email;
import ezvcard.property.Telephone;
import ezvcard.property.TextProperty;
import ezvcard.property.VCardProperty;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.cloud.local.InMemoryJobDataCache;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.contacts.ContactsModelWrapper;
import org.dataportabilityproject.serviceProviders.google.GooglePaginationInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleContactsServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(GoogleContactsServiceTest.class);

  private static final FieldMetadata PRIMARY_FIELD_METADATA = new FieldMetadata().setPrimary(true);
  private static final FieldMetadata SECONDARY_FIELD_METADATA =
      new FieldMetadata().setPrimary(false);

  private static final String RESOURCE_NAME = "resource_name";

  private static final PhoneNumber PRIMARY_PHONE = new PhoneNumber().setValue("334-844-4244")
      .setMetadata(PRIMARY_FIELD_METADATA);
  private static final PhoneNumber SECONDARY_PHONE = new PhoneNumber().setValue("555-867-5309")
      .setMetadata(SECONDARY_FIELD_METADATA);

  private static final String FIRST_NAME = "Jane";
  private static final String LAST_NAME = "Doe";
  private static final Name NAME = new Name()
      .setGivenName(FIRST_NAME)
      .setFamilyName(LAST_NAME)
      .setMetadata(PRIMARY_FIELD_METADATA);

  private static final Person PERSON = new Person()
      .setNames(Collections.singletonList(NAME))
      .setPhoneNumbers(Arrays.asList(PRIMARY_PHONE, SECONDARY_PHONE))
      .setResourceName(RESOURCE_NAME);

  private PeopleService peopleService;
  private JobDataCache jobDataCache;
  private GoogleContactsService contactsService;

  @Before
  public void setup() {
    peopleService = mock(PeopleService.class, Mockito.RETURNS_DEEP_STUBS);
    jobDataCache = new InMemoryJobDataCache();
    contactsService = new GoogleContactsService(peopleService, jobDataCache);
  }

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
    VCard vCard = GoogleContactsService.convertPersonToVCard(person);

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
    VCard vCard = GoogleContactsService.convertPersonToVCard(person);

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

  @Test
  public void exportFirstPage() throws IOException {
    // Set up: one person in a page, with a next page token set to mimic that there's another page
    List<Person> connectionsList = Collections.singletonList(PERSON);
    String nextPageToken = "token";
    ExportInformation emptyExportInformation = new ExportInformation(Optional.empty(),
        Optional.empty()); // first page

    ListConnectionsResponse listConnectionsResponse = new ListConnectionsResponse();
    listConnectionsResponse.setConnections(connectionsList);
    listConnectionsResponse.setNextPageToken(nextPageToken);

    Connections.List listConnections = mock(Connections.List.class);

    PersonResponse personResponse = new PersonResponse().setPerson(PERSON);
    GetPeopleResponse batchResponse = new GetPeopleResponse()
        .setResponses(Collections.singletonList(personResponse));

    when(peopleService.people()
        .connections()
        .list(SELF_RESOURCE))
        .thenReturn(listConnections);
    when(listConnections.execute())
        .thenReturn(listConnectionsResponse);
    when(peopleService.people()
        .getBatchGet()
        .setResourceNames(Collections.singletonList(RESOURCE_NAME))
        .setPersonFields(PERSON_FIELDS)
        .execute())
        .thenReturn(batchResponse);

    // Run test
    ContactsModelWrapper wrapper = contactsService.export(emptyExportInformation);

    // Check the correct calls were made
    verify(peopleService).people().connections().list(SELF_RESOURCE);
    verify(listConnections).execute();
    verify(peopleService).people().getBatchGet()
        .setResourceNames(Collections.singletonList(RESOURCE_NAME)).setPersonFields(PERSON_FIELDS)
        .execute();

    // Check continuation information
    assertThat(wrapper.getContinuationInformation().getSubResources()).isEmpty();
    GooglePaginationInfo googlePaginationInfo = (GooglePaginationInfo) wrapper
        .getContinuationInformation()
        .getPaginationInformation();
    assertThat(googlePaginationInfo.getPageToken()).isEqualTo(nextPageToken);

    // Check that the right number of VCards was returned
    Collection<VCard> vCardCollection = wrapper.getVCards();
    assertThat(vCardCollection.size()).isEqualTo(connectionsList.size());
  }

  @Test
  public void exportSubsequentPage() throws IOException {
    List<Person> connectionsList = Collections.singletonList(PERSON);
    String nextPageToken = "token";
    ExportInformation nextPageExportInformation = new ExportInformation(Optional.empty(),
        Optional.of(new GooglePaginationInfo(nextPageToken)));

    ListConnectionsResponse listConnectionsResponse = new ListConnectionsResponse();
    listConnectionsResponse.setConnections(connectionsList);
    listConnectionsResponse.setNextPageToken(null);

    Connections.List listConnections = mock(Connections.List.class);

    PersonResponse personResponse = new PersonResponse().setPerson(PERSON);
    GetPeopleResponse batchResponse = new GetPeopleResponse()
        .setResponses(Collections.singletonList(personResponse));

    when(peopleService.people()
        .connections()
        .list(SELF_RESOURCE))
        .thenReturn(listConnections);
    when(listConnections.setPageToken(nextPageToken))
        .thenReturn(listConnections);
    when(listConnections.execute())
        .thenReturn(listConnectionsResponse);
    when(peopleService.people()
        .getBatchGet()
        .setResourceNames(Collections.singletonList(RESOURCE_NAME))
        .setPersonFields(PERSON_FIELDS)
        .execute())
        .thenReturn(batchResponse);

    // Run test
    ContactsModelWrapper wrapper = contactsService.export(nextPageExportInformation);

    // Verify correct calls were made - i.e., token was added before execution
    InOrder inOrder = Mockito.inOrder(listConnections);
    inOrder.verify(listConnections).setPageToken(nextPageToken);
    inOrder.verify(listConnections).execute();

    // Check continuation information
    assertThat(wrapper.getContinuationInformation().getSubResources()).isEmpty();
    assertThat(wrapper.getContinuationInformation().getPaginationInformation()).isNull();
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
