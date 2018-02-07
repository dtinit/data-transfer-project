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

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.PeopleService.People.Connections;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import com.google.common.annotations.VisibleForTesting;
import com.google.gdata.util.common.base.Pair;
import ezvcard.VCard;
import ezvcard.property.Email;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.contacts.ContactsModelWrapper;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;

public class GoogleContactsService implements Exporter<ContactsModelWrapper>,
    Importer<ContactsModelWrapper> {

  private PeopleService peopleService;
  private JobDataCache jobDataCache;

  @VisibleForTesting
  static final String SELF_RESOURCE = "people/me";

  public GoogleContactsService(Credential credential, JobDataCache jobDataCache) {
    // TODO(olsona): add permissions/scopes!
    this(new PeopleService.Builder(
        GoogleStaticObjects.getHttpTransport(), GoogleStaticObjects.JSON_FACTORY, credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build(), jobDataCache);
  }

  @VisibleForTesting
  GoogleContactsService(PeopleService peopleService, JobDataCache jobDataCache) {
    this.peopleService = peopleService;
    this.jobDataCache = jobDataCache;
  }

  @VisibleForTesting
  static VCard convertPersonToModel(Person person) throws IOException {
    VCard vcard = new VCard();

    /* Reluctant to set the VCard.Kind value, since a) there aren't that many type options for
    Google contacts, b) those type options are often wrong, and c) those type options aren't even
    reliably in the same place.
    Source: https://developers.google.com/people/api/rest/v1/people#personmetadata
    */

    Pair<StructuredName, StructuredName[]> namesPair = convertToVcardNames(person.getNames());
    vcard.setStructuredName(namesPair.first);
    vcard.setStructuredNameAlt(namesPair.second);

    /* TODO(olsona): uncomment when we want to test it
    for (Telephone telephone : convertTelephoneNumbers(person.getPhoneNumbers())) {
      vcard.addTelephoneNumber(telephone);
    }

    for (ezvcard.property.Address vcardAddress : convertToVcardAddresses(person.getAddresses())) {
      vcard.addAddress(vcardAddress);
    }

    for (Email vcardEmail : convertToVcardEmails(person.getEmailAddresses())) {
      vcard.addEmail(vcardEmail);
    }
    */

    return vcard;
  }

  @VisibleForTesting
  static List<ezvcard.property.Address> convertToVcardAddresses(
      List<com.google.api.services.people.v1.model.Address> personAddresses) {
    List<ezvcard.property.Address> vcardAddresses = new LinkedList<>();
    // TODO(olsona): all of this - can use Java 8 streams

    return vcardAddresses;
  }

  @VisibleForTesting
  static List<Email> convertToVcardEmails(List<EmailAddress> personEmails) {
    List<Email> vcardEmails = new LinkedList<>();
    for (EmailAddress personEmail : personEmails) {
      Email vcardEmail = new Email(personEmail.getValue());
      vcardEmails.add(vcardEmail);

      // TODO(olsona): address primary/secondary email
      // TODO(olsona): address Email.displayName
      // TODO(olsona): address Email.formattedType
    }

    return vcardEmails;
  }

  @VisibleForTesting
  static Pair<StructuredName, StructuredName[]> convertToVcardNames(
      List<Name> personNames) {
    StructuredName primaryVcardName = null;
    LinkedList<StructuredName> alternateVcardNames = new LinkedList<>();
    for (Name personName : personNames) {
      if (personName.getMetadata().getPrimary()) {
        // This is the primary name for the Person, so it should be the primary name in the VCard.
        primaryVcardName = convertSinglePersonName(personName);
      } else {
        alternateVcardNames.add(convertSinglePersonName(personName));
      }
    }
    if (primaryVcardName == null) {
      // No personName was set as primary, so we'll just get the first alternate name
      primaryVcardName = alternateVcardNames.pop();
    }

    StructuredName[] altArray = alternateVcardNames
        .toArray(new StructuredName[alternateVcardNames.size()]);

    return Pair.of(primaryVcardName, altArray);
  }

  @VisibleForTesting
  static StructuredName convertSinglePersonName(Name personName) {
    StructuredName structuredName = new StructuredName();
    structuredName.setFamily(personName.getFamilyName());
    structuredName.setGiven(personName.getGivenName());
    structuredName.getPrefixes().add(personName.getHonorificPrefix());
    structuredName.getSuffixes().add(personName.getHonorificSuffix());

    // TODO(olsona): address formatting, structure, phonetics
    return structuredName;
  }

  @VisibleForTesting
  static List<Telephone> convertTelephoneNumbers(List<PhoneNumber> personNumbers) {
    List<Telephone> vcardTelephones = new LinkedList<>();
    for (PhoneNumber personNumber : personNumbers) {
      Telephone telephone = new Telephone(personNumber.getValue());
      vcardTelephones.add(telephone);
    }

    return vcardTelephones;
  }

  @Override
  public ContactsModelWrapper export(ExportInformation continuationInformation) throws IOException {
    // TODO(olsona): get next page using pagination token
    Connections.List connectionsListRequest = peopleService.people().connections()
        .list(SELF_RESOURCE);
    ListConnectionsResponse response = connectionsListRequest.execute();
    List<Person> initialPeopleList = response.getConnections();
    List<VCard> vCards = new LinkedList<>();
    for (Person initialPerson : initialPeopleList) {
      Person fullPerson = peopleService.people().get(initialPerson.getResourceName()).execute();
      vCards.add(convertPersonToModel(fullPerson));
    }
    GoogleContactsP8nInfo newPage = null;
    if (response.getTotalItems() > initialPeopleList.size()) {
      newPage = new GoogleContactsP8nInfo(response.getNextPageToken());
    }
    return new ContactsModelWrapper(vCards, new ContinuationInformation(null, newPage));
  }

  @Override
  public void importItem(ContactsModelWrapper object) throws IOException {
    // TODO(olsona)
  }
}
