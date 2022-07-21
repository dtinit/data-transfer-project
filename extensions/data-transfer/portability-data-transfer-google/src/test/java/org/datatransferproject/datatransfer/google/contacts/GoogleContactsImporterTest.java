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

import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.CONTACT_SOURCE_TYPE;
import static org.datatransferproject.datatransfer.google.common.GoogleStaticObjects.SOURCE_PARAM_NAME_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleService.People;
import com.google.api.services.people.v1.PeopleService.People.CreateContact;
import com.google.api.services.people.v1.model.Person;
import ezvcard.VCard;
import ezvcard.property.StructuredName;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.types.common.models.contacts.ContactsModelWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GoogleContactsImporterTest {

  private PeopleService peopleService;
  private GoogleContactsImporter contactsService;
  private People people;
  private CreateContact createContact;
  private IdempotentImportExecutor executor;

  @BeforeEach
  public void setup() throws IOException {
    people = mock(People.class);
    peopleService = mock(PeopleService.class);
    createContact = mock(CreateContact.class);

    contactsService = new GoogleContactsImporter(peopleService);
    executor = new FakeIdempotentImportExecutor();

    when(peopleService.people()).thenReturn(people);
    when(people.createContact(any(Person.class))).thenReturn(createContact);

    Person person = new Person();
    when(createContact.execute()).thenReturn(person);
  }

  @Test
  public void importFirstResources() throws Exception {
    // Set up: small number of VCards to be imported
    int numberOfVCards = 5;
    List<VCard> vCardList = new LinkedList<>();
    for (int i = 0; i < numberOfVCards; i++) {
      StructuredName structuredName = new StructuredName();
      structuredName.setFamily("Family" + i);
      structuredName.setParameter(SOURCE_PARAM_NAME_TYPE, CONTACT_SOURCE_TYPE);
      VCard vCard = new VCard();
      vCard.setStructuredName(structuredName);
      vCard.setFormattedName("First " + structuredName.getFamily());
      vCardList.add(vCard);
    }
    String vCardString = GoogleContactsExporter.makeVCardString(vCardList);
    ContactsModelWrapper wrapper = new ContactsModelWrapper(vCardString);

    // Run test
    contactsService.importItem(UUID.randomUUID(), executor,null, wrapper);

    // Check that the right methods were called
    verify(people, times(numberOfVCards)).createContact(any(Person.class));
    verify(createContact, times(numberOfVCards)).execute();
  }
}
