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
import com.google.api.services.people.v1.model.GetPeopleResponse;
import com.google.api.services.people.v1.model.ListConnectionsResponse;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PersonResponse;
import com.google.common.annotations.VisibleForTesting;
import ezvcard.VCard;
import java.io.IOException;
import java.util.Collection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleContactsService implements Exporter<ContactsModelWrapper>,
    Importer<ContactsModelWrapper> {

  private static final Logger logger = LoggerFactory.getLogger(GoogleContactsService.class);

  private PeopleService peopleService;
  private JobDataCache jobDataCache;

  @VisibleForTesting
  static final String SELF_RESOURCE = "people/me";

  @VisibleForTesting
  // List of all fields we want to get from the Google Contacts API
  // NB: this will have to be updated as we support more fields
  static final String PERSON_FIELDS = "emailAddresses,names,phoneNumbers";
  // TODO(olsona): addresses next

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

  public ContactsModelWrapper export(ExportInformation continuationInformation) throws IOException {
    // Set up connection
    Connections.List connectionsList = peopleService
        .people()
        .connections()
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
        .map(a -> GoogleContactToVCardConverter.convert(a.getPerson()))
        .collect(Collectors.toList());

    // Determine if there's a next page
    GooglePaginationInfo newPage = null;
    if (response.getNextPageToken() != null) {
      newPage = new GooglePaginationInfo(response.getNextPageToken());
    }

    return new ContactsModelWrapper(vCards, new ContinuationInformation(null, newPage));
  }

  @Override
  public void importItem(ContactsModelWrapper wrapper) throws IOException {
    // TODO(olsona)
    // First, assume no ContinuationInformation
    Collection<VCard> vCardCollection = wrapper.getVCards();
    for (VCard vCard : vCardCollection) {
      Person person = VCardToGoogleContactConverter.convert(vCard);
      peopleService.people().createContact(person);
      logger.debug("Created contact: {}", person.getNames().get(0));
    }
  }
}
