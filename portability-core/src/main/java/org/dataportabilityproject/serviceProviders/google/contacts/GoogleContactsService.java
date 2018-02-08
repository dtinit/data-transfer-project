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
import com.google.api.services.people.v1.model.FieldMetadata;
import com.google.api.services.people.v1.model.GetPeopleResponse;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PersonResponse;
import com.google.api.services.people.v1.model.PhoneNumber;
import com.google.common.annotations.VisibleForTesting;
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

  @VisibleForTesting
  static final int PRIMARY_PREF = 1;
  @VisibleForTesting
  static final int SECONDARY_PREF = 2;

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
  static VCard convertPersonToVCard(Person person) {
    VCard vCard = new VCard();

    /* Reluctant to set the VCard.Kind value, since a) there aren't that many type options for
    Google contacts, b) those type options are often wrong, and c) those type options aren't even
    reliably in the same place.
    Source: https://developers.google.com/people/api/rest/v1/people#personmetadata
    */

    if (person.getNames() != null) {
      convertToVCardNamesAndPopulate(vCard, person.getNames());
    }

    if (person.getPhoneNumbers() != null) {
      // VCard API does not support adding multiple telephone numbers at once
      person.getPhoneNumbers().forEach(n -> vCard.addTelephoneNumber(convertToVCardTelephone(n)));
    }

    if (person.getEmailAddresses() != null) {
      // VCard API does not support adding multiple emails at once
      person.getEmailAddresses().forEach(e -> vCard.addEmail(convertToVCardEmail(e)));
    }

    return vCard;
  }

  private static int getPref(FieldMetadata metadata) {
    return metadata.getPrimary() ? PRIMARY_PREF : SECONDARY_PREF;
  }

  @VisibleForTesting
  private static Email convertToVCardEmail(EmailAddress personEmail) {
    // TODO(olsona): address Email.displayName
    // TODO(olsona): address Email.formattedType
    Email email = new Email(personEmail.getValue());
    email.setPref(getPref(personEmail.getMetadata()));

    return email;
  }

  @VisibleForTesting
  private static void convertToVCardNamesAndPopulate(VCard vCard,
      List<Name> personNames) {
    LinkedList<StructuredName> alternateVCardNames = new LinkedList<>();
    for (Name personName : personNames) {
      if (personName.getMetadata().getPrimary()) {
        // This is the primary name for the Person, so it should be the primary name in the VCard.
        vCard.setStructuredName(convertToVCardNameSingle(personName));
      } else {
        alternateVCardNames.add(convertToVCardNameSingle(personName));
      }
    }
    if (vCard.getStructuredName() == null) {
      // No personName was set as primary, so we'll just get the first alternate name
      vCard.setStructuredName(alternateVCardNames.pop());
    }

    vCard.setStructuredNameAlt(
        alternateVCardNames.toArray(new StructuredName[alternateVCardNames.size()]));
  }

  @VisibleForTesting
  private static StructuredName convertToVCardNameSingle(Name personName) {
    StructuredName structuredName = new StructuredName();
    structuredName.setFamily(personName.getFamilyName());
    structuredName.setGiven(personName.getGivenName());
    structuredName.getPrefixes().add(personName.getHonorificPrefix());
    structuredName.getSuffixes().add(personName.getHonorificSuffix());

    // TODO(olsona): address formatting, structure, phonetics
    return structuredName;
  }

  @VisibleForTesting
  private static Telephone convertToVCardTelephone(PhoneNumber personNumber) {
    Telephone telephone = new Telephone(personNumber.getValue());
    telephone.setPref(getPref(personNumber.getMetadata()));
    return telephone;
  }

  @Override
  public ContactsModelWrapper export(ExportInformation continuationInformation) throws IOException {
    // Set up connection
    Connections.List connectionsList = peopleService.people().connections()
        .list(SELF_RESOURCE);

    // Get next page, if we have a page token
    if (continuationInformation.getPaginationInformation().isPresent()) {
      String pageToken = ((GooglePaginationInfo) continuationInformation.getPaginationInformation()
          .get()).getPageToken();
      connectionsList.setPageToken(pageToken);
    }

    // Get list of connections (nb: not a list containing full info of each Person)
    ListConnectionsResponse response = connectionsList.execute();
    List<Person> peopleList = response.getConnections();

    // Get list of resource names, then get list of Persons
    List<String> resourceNames = peopleList.stream()
        .map(Person::getResourceName)
        .collect(Collectors.toList());
    GetPeopleResponse batchResponse = peopleService.people()
        .getBatchGet()
        .setResourceNames(resourceNames)
        .setPersonFields(PERSON_FIELDS)
        .execute();
    List<PersonResponse> personResponseList = batchResponse.getResponses();

    // Convert Persons to VCards
    List<VCard> vCards = personResponseList.stream()
        .map(a -> convertPersonToVCard(a.getPerson()))
        .collect(Collectors.toList());

    // Determine if there's a next page
    GooglePaginationInfo newPage = null;
    if (response.getNextPageToken() != null) {
      newPage = new GooglePaginationInfo(response.getNextPageToken());
    }

    return new ContactsModelWrapper(vCards, new ContinuationInformation(null, newPage));
  }

  @Override
  public void importItem(ContactsModelWrapper object) throws IOException {
    // TODO(olsona)
  }
}
