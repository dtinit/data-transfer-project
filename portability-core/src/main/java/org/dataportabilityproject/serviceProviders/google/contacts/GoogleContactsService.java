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
import com.google.api.services.people.v1.model.GetPeopleResponse;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PersonResponse;
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
import java.util.stream.Collectors;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.contacts.ContactsModelWrapper;
import org.dataportabilityproject.serviceProviders.google.GooglePaginationInfo;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;

public class GoogleContactsService implements Exporter<ContactsModelWrapper>,
    Importer<ContactsModelWrapper> {

  private PeopleService peopleService;
  private JobDataCache jobDataCache;

  @VisibleForTesting
  static final String SELF_RESOURCE = "people/me";

  @VisibleForTesting
  // List of all fields we want to get from the Google Contacts API
  static final String PERSON_FIELDS = "addresses,emailAddresses,names,phoneNumbers";

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
  static VCard convertPersonToModel(Person person) {
    VCard vCard = new VCard();

    /* Reluctant to set the VCard.Kind value, since a) there aren't that many type options for
    Google contacts, b) those type options are often wrong, and c) those type options aren't even
    reliably in the same place.
    Source: https://developers.google.com/people/api/rest/v1/people#personmetadata
    */

    Pair<StructuredName, StructuredName[]> namesPair = convertToVCardNames(person.getNames());
    vCard.setStructuredName(namesPair.first);
    vCard.setStructuredNameAlt(namesPair.second);

    /* TODO(olsona): uncomment when we want to test it
    for (Telephone telephone : convertToVCardTelephoneNumbers(person.getPhoneNumbers())) {
      vcard.addTelephoneNumber(telephone);
    }

    for (ezvcard.property.Address vcardAddress : convertToVCardAddresses(person.getAddresses())) {
      vcard.addAddress(vcardAddress);
    }

    for (Email vcardEmail : convertToVCardEmails(person.getEmailAddresses())) {
      vcard.addEmail(vcardEmail);
    }
    */

    return vCard;
  }

  @VisibleForTesting
  static List<ezvcard.property.Address> convertToVCardAddresses(
      List<com.google.api.services.people.v1.model.Address> personAddresses) {
    // TODO(olsona): address question of which address is primary
    return personAddresses.stream()
        .map(GoogleContactsService::convertToVCardAddressSingle)
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  static ezvcard.property.Address convertToVCardAddressSingle(
      com.google.api.services.people.v1.model.Address personAddress) {
    // TODO(olsona)
    return null;
  }

  @VisibleForTesting
  static List<Email> convertToVCardEmails(List<EmailAddress> personEmails) {
    return personEmails.stream()
        .map(GoogleContactsService::convertToVCardEmailSingle)
        .collect(Collectors.toList());
  }

  @VisibleForTesting
  static Email convertToVCardEmailSingle(EmailAddress personEmail) {
    // TODO(olsona): address primary/secondary email
    // TODO(olsona): address Email.displayName
    // TODO(olsona): address Email.formattedType

    return new Email(personEmail.getValue());
  }

  @VisibleForTesting
  static Pair<StructuredName, StructuredName[]> convertToVCardNames(
      List<Name> personNames) {
    StructuredName primaryVCardName = null;
    LinkedList<StructuredName> alternateVCardNames = new LinkedList<>();
    for (Name personName : personNames) {
      if (personName.getMetadata().getPrimary()) {
        // This is the primary name for the Person, so it should be the primary name in the VCard.
        primaryVCardName = convertToVCardNameSingle(personName);
      } else {
        alternateVCardNames.add(convertToVCardNameSingle(personName));
      }
    }
    if (primaryVCardName == null) {
      // No personName was set as primary, so we'll just get the first alternate name
      primaryVCardName = alternateVCardNames.pop();
    }

    StructuredName[] altArray = alternateVCardNames
        .toArray(new StructuredName[alternateVCardNames.size()]);

    return Pair.of(primaryVCardName, altArray);
  }

  @VisibleForTesting
  static StructuredName convertToVCardNameSingle(Name personName) {
    StructuredName structuredName = new StructuredName();
    structuredName.setFamily(personName.getFamilyName());
    structuredName.setGiven(personName.getGivenName());
    structuredName.getPrefixes().add(personName.getHonorificPrefix());
    structuredName.getSuffixes().add(personName.getHonorificSuffix());

    // TODO(olsona): address formatting, structure, phonetics
    return structuredName;
  }

  @VisibleForTesting
  static List<Telephone> convertToVCardTelephoneNumbers(List<PhoneNumber> personNumbers) {
    // TODO(olsona): address primary/secondary
    return personNumbers.stream()
        .map(a -> new Telephone(a.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  public ContactsModelWrapper export(ExportInformation continuationInformation) throws IOException {
    // TODO(olsona): get next page using pagination token, if token is present
    ListConnectionsResponse response = peopleService.people().connections().list(SELF_RESOURCE).execute();
    List<Person> initialPeopleList = response.getConnections();
    List<String> resourceNames = initialPeopleList.stream()
        .map(Person::getResourceName)
        .collect(Collectors.toList());
    GetPeopleResponse batchResponse = peopleService.people()
        .getBatchGet()
        .setResourceNames(resourceNames)
        .setPersonFields(PERSON_FIELDS)
        .execute();
    List<PersonResponse> personResponseList = batchResponse.getResponses();
    List<VCard> vCards = personResponseList.stream()
        .map(a -> convertPersonToModel(a.getPerson()))
        .collect(Collectors.toList());
    GooglePaginationInfo newPage = null;
    if (response.getTotalItems() > initialPeopleList.size()) {
      newPage = new GooglePaginationInfo(response.getNextPageToken());
    }
    return new ContactsModelWrapper(vCards, new ContinuationInformation(null, newPage));
  }

  @Override
  public void importItem(ContactsModelWrapper object) throws IOException {
    // TODO(olsona)
  }
}
