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
    .SELF_RESOURCE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleService.People.Connections;
import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.GetPeopleResponse;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
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

public class GoogleContactsServiceTest {

  private static final String RESOURCE_NAME = "resource_name";
  private static final FieldMetadata PRIMARY_FIELD_METADATA = new FieldMetadata().setPrimary(true);
  private static final Name NAME = new Name().setFamilyName("Turing").setGivenName("Alan")
      .setMetadata(PRIMARY_FIELD_METADATA);
  private static final Person PERSON = new Person().setNames(Collections.singletonList(NAME))
      .setResourceName(RESOURCE_NAME);

  private static final String NEXT_PAGE_TOKEN = "nextPageToken";

  private PeopleService peopleService;
  private JobDataCache jobDataCache;
  private GoogleContactsService contactsService;

  private List<Person> connectionsList;
  private Connections.List listConnectionsRequest;
  private ListConnectionsResponse listConnectionsResponse;
  private PersonResponse personResponse;
  private GetPeopleResponse batchResponse;

  @Before
  public void setup() throws IOException {
    peopleService = mock(PeopleService.class, Mockito.RETURNS_DEEP_STUBS);
    jobDataCache = new InMemoryJobDataCache();
    contactsService = new GoogleContactsService(peopleService, jobDataCache);
    listConnectionsRequest = mock(Connections.List.class);

    when(peopleService.people()
        .connections()
        .list(SELF_RESOURCE))
        .thenReturn(listConnectionsRequest);
  }

  private void setUpSinglePersonResponse() throws IOException {
    connectionsList = Collections.singletonList(PERSON);
    listConnectionsResponse = new ListConnectionsResponse();
    listConnectionsResponse.setConnections(connectionsList);
    personResponse = new PersonResponse().setPerson(PERSON);
    batchResponse = new GetPeopleResponse().setResponses(Collections.singletonList(personResponse));

    // This can't go in setup()
    when(listConnectionsRequest.execute()).thenReturn(listConnectionsResponse);
    // This is specific to returning a single Person
    when(peopleService.people()
        .getBatchGet()
        .setResourceNames(Collections.singletonList(RESOURCE_NAME))
        .setPersonFields(PERSON_FIELDS)
        .execute())
        .thenReturn(batchResponse);
  }

  @Test
  public void exportFirstPage() throws IOException {
    setUpSinglePersonResponse();

    // Looking at first page, with at least one page after it
    ExportInformation emptyExportInformation = new ExportInformation(Optional.empty(),
        Optional.empty());
    listConnectionsResponse.setNextPageToken(NEXT_PAGE_TOKEN);

    // Run test
    ContactsModelWrapper wrapper = contactsService.export(emptyExportInformation);

    // Check continuation information
    assertThat(wrapper.getContinuationInformation().getSubResources()).isEmpty();
    GooglePaginationInfo googlePaginationInfo = (GooglePaginationInfo) wrapper
        .getContinuationInformation()
        .getPaginationInformation();
    assertThat(googlePaginationInfo.getPageToken()).isEqualTo(NEXT_PAGE_TOKEN);

    // Check that the right number of VCards was returned
    Collection<VCard> vCardCollection = wrapper.getVCards();
    assertThat(vCardCollection.size()).isEqualTo(connectionsList.size());
  }

  @Test
  public void exportSubsequentPage() throws IOException {
    setUpSinglePersonResponse();

    // Looking at a subsequent page, with no pages after it
    ExportInformation nextPageExportInformation = new ExportInformation(Optional.empty(),
        Optional.of(new GooglePaginationInfo(NEXT_PAGE_TOKEN)));
    listConnectionsResponse.setNextPageToken(null);

    when(listConnectionsRequest.setPageToken(NEXT_PAGE_TOKEN)).thenReturn(listConnectionsRequest);

    // Run test
    ContactsModelWrapper wrapper = contactsService.export(nextPageExportInformation);

    // Verify correct calls were made - i.e., token was added before execution
    InOrder inOrder = Mockito.inOrder(listConnectionsRequest);
    inOrder.verify(listConnectionsRequest).setPageToken(NEXT_PAGE_TOKEN);
    inOrder.verify(listConnectionsRequest).execute();

    // Check continuation information
    assertThat(wrapper.getContinuationInformation().getSubResources()).isEmpty();
    assertThat(wrapper.getContinuationInformation().getPaginationInformation()).isNull();
  }

  @Test
  public void importFirstResources() {
    // TODO(olsona)
  }
}
