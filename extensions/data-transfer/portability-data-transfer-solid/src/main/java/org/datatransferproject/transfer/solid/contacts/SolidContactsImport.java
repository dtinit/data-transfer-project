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

package org.datatransferproject.transfer.solid.contacts;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;
import org.datatransferproject.spi.transfer.provider.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.solid.SolidUtilities;
import org.datatransferproject.types.common.models.contacts.ContactsModelWrapper;
import org.datatransferproject.types.transfer.auth.CookiesAndUrlAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;

public class SolidContactsImport implements Importer<CookiesAndUrlAuthData, ContactsModelWrapper> {
  //See https://www.w3.org/TR/vcard-rdf/ for details.

  private static final Logger logger = LoggerFactory.getLogger(SolidContactsExport.class);
  private static final String TEST_SLUG_NAME = "ImportedAddressBook";
  private static final String BASE_DIRECTORY = "/inbox/";
  private static final String BASIC_CONTAINER_TYPE = "http://www.w3.org/ns/ldp#BasicContainer";
  private static final String BASIC_RESOURCE_TYPE = "http://www.w3.org/ns/ldp#Resource";


  @SuppressWarnings("deprecation")
  private static final ImmutableList<Resource> EMAIL_TYPE_RESOURCES = ImmutableList.of(
      VCARD4.Home,
      VCARD4.Work,
      // Deprecated Types:
      VCARD4.Dom,
      VCARD4.Internet,
      VCARD4.ISDN,
      VCARD4.Pref);
  private static final ImmutableMap<String, Resource> MAP_OF_EMAIL_TYPES;

  @SuppressWarnings("deprecation")
  private static final ImmutableList<Resource> PHONE_TYPE_RESOURCES = ImmutableList.of(
      VCARD4.Cell,
      VCARD4.Fax,
      VCARD4.Home,
      VCARD4.Pager,
      VCARD4.Pref,
      VCARD4.Text,
      VCARD4.TextPhone,
      VCARD4.Video,
      VCARD4.Voice,
      VCARD4.Work);
  private static final ImmutableMap<String, Resource> MAP_OF_PHONE_TYPES;


  static {
    MAP_OF_EMAIL_TYPES = ImmutableMap.copyOf(EMAIL_TYPE_RESOURCES.stream()
        .collect(Collectors.toMap(
            r -> r.getLocalName().toLowerCase(),
            Function.identity()
    )));

    MAP_OF_PHONE_TYPES = ImmutableMap.copyOf(PHONE_TYPE_RESOURCES.stream()
        .collect(Collectors.toMap(
            r -> r.getLocalName().toLowerCase(),
            Function.identity()
        )));
  }

  @VisibleForTesting
  static final String IMPORTED_ADDRESS_BOOK_PATH = BASE_DIRECTORY + TEST_SLUG_NAME + "/";

  @Override
  public ImportResult importItem(
      UUID jobId,
      IdempotentImportExecutor idempotentExecutor,
      CookiesAndUrlAuthData authData,
      ContactsModelWrapper data) throws Exception {
    checkState(authData.getCookies().size() == 1,
        "Exactly 1 cookie expected: %s",
        authData.getCookies());

    SolidUtilities solidUtilities = new SolidUtilities(authData.getCookies().get(0));

    String url = authData.getUrl();

    List<VCard> vcards = Ezvcard.parse(data.getVCards()).all();
    createContent(idempotentExecutor, url, vcards, solidUtilities);
    return ImportResult.OK;
  }

  private void createContent(
      IdempotentImportExecutor idempotentExecutor,
      String baseUrl,
      List<VCard> people,
      SolidUtilities utilities)
      throws Exception {
    String addressBookSlug = TEST_SLUG_NAME;

    String containerUrl = createContainer(baseUrl + BASE_DIRECTORY, addressBookSlug, utilities);

    idempotentExecutor.execute(baseUrl + containerUrl,
        addressBookSlug,
        () -> createIndex(baseUrl + containerUrl, addressBookSlug, utilities));

    String personDirectory = idempotentExecutor.execute(
        baseUrl + containerUrl + "person",
        addressBookSlug,
        () -> createPersonDirectory(baseUrl + containerUrl, utilities));

    Map<String, VCard> insertedPeople = people.stream()
        .collect(Collectors.toMap(
            p -> importPerson(idempotentExecutor, p, baseUrl, personDirectory, utilities),
            Function.identity()));

    idempotentExecutor.execute(
        "peopleFile",
        addressBookSlug,
        () -> createPeopleFile(baseUrl, containerUrl, insertedPeople, utilities));
  }

  private String importPerson(IdempotentImportExecutor executor,
      VCard person,
      String baseUrl,
      String personDirectory,
      SolidUtilities utilities) {
    try {
    return executor.execute(
        Integer.toString(person.hashCode()),
        person.getFormattedName().getValue(),
        () -> insertPerson(baseUrl, personDirectory, person, utilities));
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private String createContainer(String url, String slug, SolidUtilities utilities) throws Exception {
    Model containerModel = ModelFactory.createDefaultModel();
    Resource containerResource = containerModel.createResource("");
    containerResource.addProperty(DCTerms.title, slug);
    return utilities.postContent(url, slug, BASIC_CONTAINER_TYPE, containerModel);
  }

  private String createIndex(String url, String slug, SolidUtilities utilities) throws Exception {
    Model model = ModelFactory.createDefaultModel();
    Resource containerResource = model.createResource("#this");
    containerResource.addProperty(RDF.type, model.getResource(VCARD4.NS + "AddressBook"));
    containerResource.addProperty(
        model.createProperty(VCARD4.NS + "nameEmailIndex"),
        model.createResource("people.ttl"));
    containerResource.addProperty(
        model.createProperty(VCARD4.NS + "groupIndex"),
        model.createResource("groups.ttl"));
    containerResource.addProperty(DC_11.title, slug);
    return utilities.postContent(
        url,
        "index",
        BASIC_RESOURCE_TYPE,
        model);
  }

  private String createPersonDirectory(String url, SolidUtilities utilities) throws IOException {
    Model personDirectoryModel = ModelFactory.createDefaultModel();
    personDirectoryModel.createResource("");
    return utilities.postContent(
        url,
        "Person",
        BASIC_CONTAINER_TYPE,
        personDirectoryModel);
  }

  private String insertPerson(String baseUrl, String container, VCard person,  SolidUtilities utilities) {
    Model personContainerModel = ModelFactory.createDefaultModel();
    personContainerModel.createResource("");
    try {
      String directory = utilities.postContent(
          baseUrl + container,
          null,
          BASIC_CONTAINER_TYPE,
          personContainerModel);

      return utilities.postContent(
          baseUrl + directory,
          "index",
          BASIC_RESOURCE_TYPE,
          getPersonModel(person));
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't insert: " + person.getFormattedName()
          + " into: " + baseUrl + container, e);
    }
  }

  private String createPeopleFile(
      String baseUrl,
      String containerUrl,
      Map<String, VCard> importedPeople,
      SolidUtilities utilities) throws Exception {
    Model peopleModel = ModelFactory.createDefaultModel();
    Resource indexResource = peopleModel.createResource("index.ttl#this");

    for (String insertedId : importedPeople.keySet()) {

      VCard insertedPerson = importedPeople.get(insertedId);
      String relativePath = insertedId.replace(containerUrl, "");

      Resource personResource = peopleModel.createResource(relativePath + "#this");
      if (insertedPerson.getFormattedName() != null) {
        personResource.addProperty(VCARD4.fn, insertedPerson.getFormattedName().getValue());
      }
      personResource.addProperty(
          peopleModel.createProperty(VCARD4.NS, "inAddressBook"),
          indexResource);
    }

    return utilities.postContent(
        baseUrl + containerUrl,
        "people",
        BASIC_RESOURCE_TYPE,
        peopleModel);
  }


  @VisibleForTesting
  final static Model getPersonModel(VCard vcard) {
    Model personModel = ModelFactory.createDefaultModel();
    Resource r = personModel.createResource("#this");
    r.addProperty(RDF.type, VCARD4.Individual);

    if (null != vcard.getFormattedName()) {
      r.addProperty(VCARD4.fn, vcard.getFormattedName().getValue());
    }

    for (StructuredName structuredName : vcard.getStructuredNames()) {
      Resource strucName = personModel.createResource();
      if (!Strings.isNullOrEmpty(structuredName.getFamily())) {
        strucName.addProperty(VCARD4.family_name, structuredName.getFamily());
      }
      if (!Strings.isNullOrEmpty(structuredName.getGiven())) {
        strucName.addProperty(VCARD4.given_name, structuredName.getGiven());
      }

      structuredName.getPrefixes()
          .forEach(prefix -> strucName.addProperty(VCARD4.hasHonorificPrefix, prefix));

      structuredName.getSuffixes()
          .forEach(suffix -> strucName.addProperty(VCARD4.hasHonorificSuffix, suffix));

      structuredName.getAdditionalNames()
          .forEach(additional -> strucName.addProperty(VCARD4.hasAdditionalName, additional));

      r.addProperty(VCARD4.hasName, strucName);
    }

    for (Email email : vcard.getEmails()) {
      String mailTo = "mailto:" + email.getValue();
      if (email.getTypes().isEmpty()) {
        r.addProperty(VCARD4.hasEmail, mailTo);
      } else {
        Resource emailResource = personModel.createResource();
        emailResource.addProperty(VCARD4.value, mailTo);
        for (EmailType type : email.getTypes()) {
          for (Resource emailTypeResource :
              getPhoneOrMailTypes(type.getValue(), MAP_OF_EMAIL_TYPES)) {
            emailResource.addProperty(RDF.type, emailTypeResource);
          }
        }
        r.addProperty(VCARD4.hasEmail, emailResource);
      }
    }

    for (Telephone telephone : vcard.getTelephoneNumbers()) {
      if (telephone.getTypes().isEmpty()) {
        r.addProperty(VCARD4.hasTelephone, telephone.getText());
      } else {
        Resource telephoneResource = personModel.createResource();
        telephoneResource.addProperty(VCARD4.value, telephone.getText());
        for (TelephoneType type : telephone.getTypes()) {
          for (Resource telTypeResource : getPhoneOrMailTypes(type.getValue(), MAP_OF_PHONE_TYPES))
            telephoneResource.addProperty(RDF.type, telTypeResource);
        }
        r.addProperty(VCARD4.hasTelephone, telephoneResource);
      }
    }

    if (vcard.getOrganization() != null) {
      r.addProperty(VCARD4.organization_name, vcard.getOrganization().getValues().get(0));
    }

    for (Organization organization : vcard.getOrganizations()) {
      organization.getValues().stream().forEach(
          v -> r.addProperty(VCARD4.hasOrganizationName, v));
    }

    for (Url url : vcard.getUrls()) {
      r.addProperty(VCARD4.hasURL, url.getValue());
    }

    for (Note note : vcard.getNotes()) {
      r.addProperty(VCARD4.hasNote, note.getValue());
    }

    for (Photo photo : vcard.getPhotos()) {
      r.addProperty(VCARD4.hasPhoto, photo.getUrl());
    }

    return personModel;
  }

  /** Looks up the {@link Resource}s for a given string, that might be comma delimited. **/
  private static ImmutableList<Resource> getPhoneOrMailTypes(
      String type,
      Map<String, Resource> map) {
    return ImmutableList.copyOf(
        Arrays.stream(type.split(",")).map(t -> {
          Resource r = map.get(t.toLowerCase());
          if (r == null) {
            logger.warn("%s didn't contain '%s' from %s", map, t.toLowerCase(), type);
            r = ModelFactory.createDefaultModel().getResource(VCARD4.NS + t);
          }
          return  r;
        })
    .collect(Collectors.toList()));
  }
}
