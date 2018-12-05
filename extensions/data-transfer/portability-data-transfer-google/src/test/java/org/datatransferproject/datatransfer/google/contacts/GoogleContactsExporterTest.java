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

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleService.People;
import com.google.api.services.people.v1.PeopleService.People.Connections;
import com.google.api.services.people.v1.PeopleService.People.GetBatchGet;
import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.GetPeopleResponse;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PersonResponse;
import com.google.api.services.people.v1.model.Source;
import ezvcard.VCard;
import ezvcard.io.json.JCardReader;
import java.util.Optional;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.contacts.ContactsModelWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.PERSON_FIELDS;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.SELF_RESOURCE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GoogleContactsExporterTest {

  private static final String RESOURCE_NAME = "resource_name";
  private static final Source SOURCE = new Source().setType("CONTACT");
  private static final FieldMetadata PRIMARY_FIELD_METADATA =
      new FieldMetadata().setSource(SOURCE).setPrimary(true);
  private static final Name NAME =
      new Name().setFamilyName("Turing").setGivenName("Alan").setMetadata(PRIMARY_FIELD_METADATA);
  private static final Person PERSON =
      new Person().setNames(Collections.singletonList(NAME)).setResourceName(RESOURCE_NAME);

  private static final String NEXT_PAGE_TOKEN = "nextPageToken";

  private PeopleService peopleService;
  private GoogleContactsExporter contactsService;
  private People people;
  private Connections connections;
  private List<Person> connectionsList;
  private GetBatchGet getBatchGet;
  private Connections.List listConnectionsRequest;
  private ListConnectionsResponse listConnectionsResponse;

  @Before
  public void setup() throws IOException {
    connections = mock(Connections.class);
    getBatchGet = mock(GetBatchGet.class);
    people = mock(People.class);
    peopleService = mock(PeopleService.class);
    listConnectionsRequest = mock(Connections.List.class);

    contactsService = new GoogleContactsExporter(peopleService);

    when(getBatchGet.setPersonFields(PERSON_FIELDS)).thenReturn(getBatchGet);
    when(people.connections()).thenReturn(connections);
    when(people.getBatchGet()).thenReturn(getBatchGet);
    when(peopleService.people()).thenReturn(people);
  }

  @Test
  public void exportFirstPage() throws IOException {
    setUpSinglePersonResponse();

    // Looking at first page, with at least one page after it
    listConnectionsResponse.setNextPageToken(NEXT_PAGE_TOKEN);

    ExportResult<ContactsModelWrapper> result = contactsService.export(UUID.randomUUID(), null, Optional.empty());

    // Check that correct methods were called
    verify(connections).list(SELF_RESOURCE);
    InOrder inOrder = Mockito.inOrder(getBatchGet);
    inOrder.verify(getBatchGet).setResourceNames(Collections.singletonList(RESOURCE_NAME));
    inOrder.verify(getBatchGet).setPersonFields(PERSON_FIELDS);
    inOrder.verify(getBatchGet).execute();

    // Check continuation data
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    assertThat(continuationData.getContainerResources()).isEmpty();
    StringPaginationToken paginationToken =
        (StringPaginationToken)
            ((ContinuationData) result.getContinuationData()).getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(NEXT_PAGE_TOKEN);

    // Check that the right number of VCards was returned
    JCardReader reader = new JCardReader(result.getExportedData().getVCards());
    List<VCard> vCardList = reader.readAll();
    assertThat(vCardList.size()).isEqualTo(connectionsList.size());
  }

  @Test
  public void exportSubsequentPage() throws IOException {
    setUpSinglePersonResponse();

    // Looking at a subsequent page, with no pages after it
    PaginationData paginationData = new StringPaginationToken(NEXT_PAGE_TOKEN);
    ExportInformation exportInformation = new ExportInformation(paginationData, null);
    listConnectionsResponse.setNextPageToken(null);

    when(listConnectionsRequest.setPageToken(NEXT_PAGE_TOKEN)).thenReturn(listConnectionsRequest);

    // Run test
    ExportResult<ContactsModelWrapper> result =
        contactsService.export(UUID.randomUUID(), null, Optional.of(exportInformation));

    // Verify correct calls were made - i.e., token was added before execution
    InOrder inOrder = Mockito.inOrder(listConnectionsRequest);
    inOrder.verify(listConnectionsRequest).setPageToken(NEXT_PAGE_TOKEN);
    inOrder.verify(listConnectionsRequest).execute();

    // Check continuation data
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    assertThat(continuationData.getContainerResources()).isEmpty();
    assertThat(continuationData.getPaginationData()).isNull();
  }

  private void setUpSinglePersonResponse() throws IOException {
    connectionsList = Collections.singletonList(PERSON);
    listConnectionsResponse = new ListConnectionsResponse();
    listConnectionsResponse.setConnections(connectionsList);
    PersonResponse personResponse = new PersonResponse().setPerson(PERSON);
    GetPeopleResponse batchResponse =
        new GetPeopleResponse().setResponses(Collections.singletonList(personResponse));

    // This can't go in setup()
    when(listConnectionsRequest.setPersonFields(PERSON_FIELDS)).thenReturn(listConnectionsRequest);
    when(listConnectionsRequest.execute()).thenReturn(listConnectionsResponse);

    // This is specific to returning a single Person
    when(connections.list(SELF_RESOURCE)).thenReturn(listConnectionsRequest);
    when(getBatchGet.setResourceNames(Collections.singletonList(RESOURCE_NAME)))
        .thenReturn(getBatchGet);
    when(getBatchGet.execute()).thenReturn(batchResponse);
  }
}
