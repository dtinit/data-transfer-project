package org.dataportabilityproject.datatransfer.google.contacts;

import static org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects.CONTACT_SOURCE_TYPE;
import static org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects.PERSON_FIELDS;
import static org.dataportabilityproject.datatransfer.google.common.GoogleStaticObjects.SOURCE_PARAM_NAME_TYPE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleService.People;
import com.google.api.services.people.v1.PeopleService.People.Connections;
import com.google.api.services.people.v1.PeopleService.People.CreateContact;
import com.google.api.services.people.v1.PeopleService.People.GetBatchGet;
import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.Source;
import ezvcard.VCard;
import ezvcard.property.StructuredName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.dataportabilityproject.types.transfer.models.contacts.ContactsModelWrapper;
import org.junit.Before;
import org.junit.Test;

public class GoogleContactsImporterTest {
  private static final String RESOURCE_NAME = "resource_name";
  private static final Source SOURCE = new Source().setType("CONTACT");
  private static final FieldMetadata PRIMARY_FIELD_METADATA =
      new FieldMetadata().setSource(SOURCE).setPrimary(true);
  private static final Name NAME =
      new Name().setFamilyName("Turing").setGivenName("Alan").setMetadata(PRIMARY_FIELD_METADATA);
  private static final Person PERSON =
      new Person().setNames(Collections.singletonList(NAME)).setResourceName(RESOURCE_NAME);

  private PeopleService peopleService;
  private GoogleContactsImporter contactsService;
  private People people;
  private CreateContact createContact;

  @Before
  public void setup() throws IOException {
    people = mock(People.class);
    peopleService = mock(PeopleService.class);
    createContact = mock(CreateContact.class);

    contactsService = new GoogleContactsImporter(peopleService);

    when(peopleService.people()).thenReturn(people);
    when(people.createContact(any(Person.class))).thenReturn(createContact);
  }

  @Test
  public void importFirstResources() throws IOException {
    // Set up: small number of VCards to be imported
    int numberOfVCards = 5;
    List<VCard> vCardList = new LinkedList<>();
    for (int i = 0; i < numberOfVCards; i++) {
      StructuredName structuredName = new StructuredName();
      structuredName.setFamily("Family" + i);
      structuredName.setParameter(SOURCE_PARAM_NAME_TYPE, CONTACT_SOURCE_TYPE);
      VCard vCard = new VCard();
      vCard.setStructuredName(structuredName);
      vCardList.add(vCard);
    }
    String vCardString = GoogleContactsExporter.makeVCardString(vCardList);
    ContactsModelWrapper wrapper = new ContactsModelWrapper(vCardString);

    // Run test
    contactsService.importItem("jobId", null, wrapper);

    // Check that the right methods were called
    verify(people, times(numberOfVCards)).createContact(any(Person.class));
    verify(createContact, times(numberOfVCards)).execute();
  }
}
