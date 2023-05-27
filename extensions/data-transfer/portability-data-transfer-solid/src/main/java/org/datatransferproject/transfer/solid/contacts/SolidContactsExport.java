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

import com.google.re2j.Matcher;
import com.google.re2j.Pattern;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Email;
import ezvcard.property.Note;
import ezvcard.property.Photo;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Url;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.ExportResult.ResultType;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.transfer.solid.SolidUtilities;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.contacts.ContactsModelWrapper;
import org.datatransferproject.types.transfer.auth.CookiesAndUrlAuthData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static com.google.common.base.Preconditions.checkState;

public class SolidContactsExport implements Exporter<CookiesAndUrlAuthData, ContactsModelWrapper> {

    private static final Pattern MAIL_TO_PATTERN = Pattern.compile("mailto:(.+@.+\\..+)");

    private static final Logger logger = LoggerFactory.getLogger(SolidContactsExport.class);

    private static final Property NAME_EMAIL_INDEX_PROPERTY = ModelFactory.createDefaultModel().createProperty(VCARD4.NS, "nameEmailIndex");

    @Override
    public ExportResult<ContactsModelWrapper> export(UUID jobId, CookiesAndUrlAuthData authData, Optional<ExportInformation> exportInformation) throws Exception {
        if (exportInformation.isPresent()) {
            throw new IllegalStateException("Currently solid doesn't support paged exports");
        }
        checkState(authData.getCookies().size() == 1, "Exactly 1 cookie expected: %s", authData.getCookies());
        SolidUtilities solidUtilities = new SolidUtilities(authData.getCookies().get(0));
        String url = authData.getUrl();
        List<List<VCard>> vCards = explore(url, solidUtilities);
        //TODO: This flattens all the address books together, which isn't perfect.
        List<VCard> allCards = new ArrayList<>();
        vCards.forEach(allCards::addAll);
        return new ExportResult<>(ResultType.END, new ContactsModelWrapper(Ezvcard.write(allCards).go()));
    }

    private List<List<VCard>> explore(String url, SolidUtilities utilities) throws IOException {
        logger.debug("Exploring: %s", url);
        List<List<VCard>> results = new ArrayList<>();
        utilities.explore(url, r -> {
            try {
                List<VCard> cards = parseAddressBookIfApplicable(r, utilities);
                if (cards != null) {
                    results.add(cards);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Problem parsing " + r.getURI(), e);
            }
        });
        return results;
    }

    private List<VCard> parseAddressBookIfApplicable(Resource resource, SolidUtilities utilities) throws IOException {
        if (SolidUtilities.isType(resource, "http://www.w3.org/2006/vcard/ns#AddressBook")) {
            logger.debug("Got Address book at %s", resource.getURI());
            return parseAddressBook(resource, utilities);
        }
        return null;
    }

    private List<VCard> parseAddressBook(Resource selfResource, SolidUtilities utilities) throws IOException {
        String peopleUri = selfResource.getProperty(NAME_EMAIL_INDEX_PROPERTY).getResource().getURI();
        Model peopleModel = utilities.getModel(peopleUri);
        List<VCard> vcards = new ArrayList<>();
        ResIterator subjects = peopleModel.listSubjects();
        while (subjects.hasNext()) {
            Resource subject = subjects.nextResource();
            Model personModel = utilities.getModel(subject.getURI());
            Resource personResource = SolidUtilities.getResource(subject.getURI(), personModel);
            if (personResource == null) {
                throw new IllegalStateException(subject.getURI() + " not found in " + subject.toString());
            }
            vcards.add(parsePerson(personResource));
        }
        return vcards;
    }

    final static VCard parsePerson(Resource r) {
        VCard vcard = new VCard();
        if (r.hasProperty(VCARD4.fn)) {
            vcard.setFormattedName(r.getProperty(VCARD4.fn).getString());
        }
        if (r.hasProperty(VCARD4.hasName)) {
            StmtIterator nameIterator = r.listProperties(VCARD4.hasName);
            while (nameIterator.hasNext()) {
                Statement nameStatement = nameIterator.nextStatement();
                Resource structuredNameResource = nameStatement.getResource();
                StructuredName structuredName = new StructuredName();
                if (structuredNameResource.hasProperty(VCARD4.family_name)) {
                    structuredName.setFamily(structuredNameResource.getProperty(VCARD4.family_name).getString());
                }
                if (structuredNameResource.hasProperty(VCARD4.given_name)) {
                    structuredName.setGiven(structuredNameResource.getProperty(VCARD4.given_name).getString());
                }
                structuredNameResource.listProperties(VCARD4.hasHonorificPrefix).forEachRemaining(prefix -> structuredName.getPrefixes().add(prefix.getString()));
                structuredNameResource.listProperties(VCARD4.hasHonorificSuffix).forEachRemaining(suffix -> structuredName.getSuffixes().add(suffix.getString()));
                structuredNameResource.listProperties(VCARD4.hasAdditionalName).forEachRemaining(additional -> structuredName.getAdditionalNames().add(additional.getString()));
                vcard.getStructuredNames().add(structuredName);
            }
        }
        if (r.hasProperty(VCARD4.organization_name)) {
            vcard.setOrganization(r.getProperty(VCARD4.organization_name).getString());
        }
        if (r.hasProperty(VCARD4.hasEmail)) {
            r.listProperties(VCARD4.hasEmail).forEachRemaining(emailStatement -> {
                Resource emailResource = emailStatement.getResource();
                if (emailResource.isResource()) {
                    Statement valueStatement = getValueStatement(emailResource);
                    String mailTo = valueStatement.getObject().toString();
                    Matcher matcher = MAIL_TO_PATTERN.matcher(mailTo);
                    checkState(matcher.matches(), "%s mail to address doesn't match", mailTo);
                    String emailAddress = matcher.group(1);
                    Email email = new Email(emailAddress);
                    if (emailResource.hasProperty(RDF.type)) {
                        emailResource.listProperties(RDF.type).forEachRemaining(typeProperty -> email.getTypes().add(EmailType.find(typeProperty.getResource().getLocalName())));
                    }
                    vcard.addEmail(email);
                } else {
                    String mailTo = emailResource.getURI();
                    Matcher matcher = MAIL_TO_PATTERN.matcher(mailTo);
                    checkState(matcher.matches(), "%s mail to address doesn't match", mailTo);
                    String emailAddress = matcher.group(1);
                    Email email = new Email(emailAddress);
                    vcard.addEmail(email);
                }
            });
        }
        if (r.hasProperty(VCARD4.hasTelephone)) {
            r.listProperties(VCARD4.hasTelephone).forEachRemaining(telephoneStatement -> {
                Resource telephoneResource = telephoneStatement.getResource();
                if (telephoneResource.isResource()) {
                    Statement valueStatement = getValueStatement(telephoneResource);
                    String telephoneNumber = valueStatement.getObject().toString();
                    Telephone telephoneObject = new Telephone(telephoneNumber);
                    if (telephoneResource.hasProperty(RDF.type)) {
                        telephoneResource.listProperties(RDF.type).forEachRemaining(typeProperty -> telephoneObject.getTypes().add(TelephoneType.find(typeProperty.getResource().getLocalName())));
                    }
                    vcard.addTelephoneNumber(telephoneObject);
                } else {
                    String telephoneNumber = telephoneResource.getURI();
                    Telephone telephoneObject = new Telephone(telephoneNumber);
                    vcard.addTelephoneNumber(telephoneObject);
                }
            });
        }
        if (r.hasProperty(VCARD4.note)) {
            vcard.addNote(r.getProperty(VCARD4.note).getString());
        }
        r.listProperties(VCARD4.hasNote).forEachRemaining(noteStatement -> vcard.getNotes().add(new Note(noteStatement.getString())));
        r.listProperties(VCARD4.hasURL).forEachRemaining(urlStatement -> vcard.getUrls().add(new Url(urlStatement.getString())));
        r.listProperties(VCARD4.hasPhoto).forEachRemaining(photoStatement -> vcard.getPhotos().add(new Photo(photoStatement.getString(), ImageType.JPEG)));
        return vcard;
    }

    private static Statement getValueStatement(Resource r) {
        Statement valueStatement = r.getProperty(VCARD4.hasValue);
        if (valueStatement == null) {
            valueStatement = r.getProperty(VCARD4.value);
        }
        if (valueStatement == null) {
            throw new IllegalStateException("Couldn't find value property in: " + r);
        }
        return valueStatement;
    }
}
