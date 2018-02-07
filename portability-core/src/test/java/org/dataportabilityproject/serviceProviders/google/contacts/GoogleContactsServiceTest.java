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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleService.People.Connections;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import ezvcard.VCard;
import ezvcard.property.Email;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.cloud.local.InMemoryJobDataCache;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.contacts.ContactsModelWrapper;
import org.dataportabilityproject.serviceProviders.google.GooglePaginationInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleContactsServiceTest {

  private static final Logger logger = LoggerFactory.getLogger(GoogleContactsServiceTest.class);

  private static final String FIRST_NAME = "Jane";
  private static final String LAST_NAME = "Doe";
  private static final String RESOURCE_NAME = "resource_name";
  private static final FieldMetadata PRIMARY_FIELD_METADATA = new FieldMetadata().setPrimary(true);
  private static final Name NAME = new Name()
      .setGivenName(FIRST_NAME)
      .setFamilyName(LAST_NAME)
      .setMetadata(PRIMARY_FIELD_METADATA);
  private static final Person PERSON = new Person()
      .setNames(Collections.singletonList(NAME))
      .setResourceName(RESOURCE_NAME);

  private Connections.List connectionsListRequest;
  private PeopleService peopleService;
  private JobDataCache jobDataCache;
  private GoogleContactsService contactsService;

  @Before
  public void setup() {
    connectionsListRequest = mock(Connections.List.class);
    peopleService = mock(PeopleService.class, Mockito.RETURNS_DEEP_STUBS);
    jobDataCache = new InMemoryJobDataCache();
    contactsService = new GoogleContactsService(peopleService, jobDataCache);
  }

  @Test
  public void convertToVCardEmail_Single() {
    // Set up test: single email
    String emailAddress = "email@google.com";
    EmailAddress googleEmail = new EmailAddress().setValue(emailAddress);

    // Perform conversion
    Email vCardEmail = GoogleContactsService.convertToVCardEmailSingle(googleEmail);
  }

  @Test
  public void convertToVCardEmails() {
    // Set up test
    int numEmails = 4;
    List<EmailAddress> googleEmails = new LinkedList<>();
    for (int i = 0; i < numEmails; i++) {
      String emailAddress = "email" + i + "@gmail.com";
      googleEmails.add(new EmailAddress().setValue(emailAddress));
    }

    // Run test
    List<Email> vCardEmails = GoogleContactsService.convertToVCardEmails(googleEmails);

    // Check that all emails were converted
    assertThat(vCardEmails.size()).isEqualTo(numEmails);
  }

  @Test
  public void exportFirstPage() throws IOException {
    // Set up
    List<Person> connectionsList = Collections.singletonList(PERSON);
    int totalItems = connectionsList.size() + 1;
    String nextPageToken = "token";
    ExportInformation emptyExportInformation = new ExportInformation(null, null);

    ListConnectionsResponse response = new ListConnectionsResponse();
    response.setConnections(connectionsList);
    response.setTotalItems(totalItems); // More than the size of the list
    response.setNextPageToken(nextPageToken);

    when(connectionsListRequest.execute()).thenReturn(response);
    when(peopleService.people().connections().list(GoogleContactsService.SELF_RESOURCE))
        .thenReturn(connectionsListRequest);
    when(peopleService.people().get(RESOURCE_NAME).execute()).thenReturn(PERSON);

    // Run test
    ContactsModelWrapper wrapper = contactsService.export(emptyExportInformation);

    // Check continuation information
    assertThat(wrapper.getContinuationInformation().getSubResources()).isEmpty();
    GooglePaginationInfo googlePaginationInfo = (GooglePaginationInfo) wrapper.getContinuationInformation()
        .getPaginationInformation();
    assertThat(googlePaginationInfo.getPageToken()).isEqualTo(nextPageToken);

    // Check VCard correctness
    Collection<VCard> vCardCollection = wrapper.getVCards();
    assertThat(vCardCollection.size()).isEqualTo(connectionsList.size());
    // TODO(olsona): check vcard correctness
  }
}
