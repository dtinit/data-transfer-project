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
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactsService.PERSON_FIELDS;
import static org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactsService.SELF_RESOURCE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleService.People.Connections;
import com.google.api.services.people.v1.model.GetPeopleResponse;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PersonResponse;
import ezvcard.VCard;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

  private static final String RESOURCE_NAME = "resource_name";

  private static final Person PERSON = new Person().setResourceName(RESOURCE_NAME);

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
    GetPeopleResponse batchResponse = new GetPeopleResponse().setResponses(Collections.singletonList(personResponse));

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

  @Test
  public void importFirstResources() {
    // TODO(olsona)
  }
}
