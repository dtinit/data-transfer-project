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
package org.datatransferproject.transfer.microsoft.transformer.contacts;

import static org.datatransferproject.transfer.microsoft.transformer.common.TransformerHelper.getList;
import static org.datatransferproject.transfer.microsoft.transformer.common.TransformerHelper.getListMap;
import static org.datatransferproject.transfer.microsoft.transformer.common.TransformerHelper.getMap;
import static org.datatransferproject.transfer.microsoft.transformer.common.TransformerHelper.getString;

import ezvcard.VCard;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Address;
import ezvcard.property.Birthday;
import ezvcard.property.Email;
import ezvcard.property.Organization;
import ezvcard.property.StructuredName;
import java.util.Map;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;

/**
 * Transforms from a Microsoft Graph contacts resource to a VCard as defined by
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/v1.0/resources/contact.
 *
 * <p>TODO: Handle contact lists (folders)
 */
public class ToVCardTransformer
    implements BiFunction<Map<String, Object>, TransformerContext, VCard> {

  @Override
  @SuppressWarnings("unchecked")
  public VCard apply(Map<String, Object> map, TransformerContext context) {
    VCard card = new VCard();

    copyNames(map, card);

    copyPersonData(map, card, context);

    copyBusinessData(map, card, context);

    getList("categories", map).ifPresent(v -> v.forEach(card::setCategories));

    getString("personalNotes", map).ifPresent(card::addNote);

    copyExtendedData(map, card);

    return card;
  }

  private void copyNames(Map<String, Object> map, VCard card) {
    String givenName = (String) map.get("givenName");
    String surname = (String) map.get("surname");
    StructuredName structuredName = new StructuredName();
    structuredName.setFamily(surname);
    structuredName.setGiven(givenName);

    getString("middleName", map).ifPresent(v -> structuredName.getAdditionalNames().add(v));

    getString("title", map).ifPresent(v -> structuredName.getPrefixes().add(v));

    card.setStructuredName(structuredName);

    getString("displayName", map).ifPresent(card::setFormattedName);

    getString("nickName", map).ifPresent(card::setNickname);
  }

  private void copyPersonData(Map<String, Object> map, VCard card, TransformerContext context) {
    getMap("homeAddress", map).ifPresent(v -> card.addAddress(context.transform(Address.class, v)));

    copyEmail(map, card);

    copyPhones("homePhones", TelephoneType.HOME, map, card);

    getString("mobilePhone", map).ifPresent(v -> card.addTelephoneNumber(v, TelephoneType.CELL));

    getString("birthday", map).ifPresent(v -> card.setBirthday(new Birthday(v)));
  }

  @SuppressWarnings("unchecked")
  private void copyEmail(Map<String, Object> map, VCard card) {
    getListMap("emailAddresses", map)
        .ifPresent(
            v ->
                v.forEach(
                    email ->
                        getString("address", email)
                            .ifPresent(addr -> card.addEmail(new Email(addr)))));
  }

  private void copyBusinessData(Map<String, Object> map, VCard card, TransformerContext context) {
    getMap("businessAddress", map)
        .ifPresent(v -> card.addAddress(context.transform(Address.class, v)));
    getMap("otherAddress", map)
        .ifPresent(v -> card.addAddress(context.transform(Address.class, v)));

    getString("jobTitle", map).ifPresent(card::addTitle);

    getString("companyName", map)
        .ifPresent(
            v -> {
              Organization organization = new Organization();
              organization.getValues().add(v);
              card.addOrganization(organization);
            });

    getString("profession", map).ifPresent(card::addExpertise);

    copyPhones("businessPhones", TelephoneType.WORK, map, card);
  }

  private void copyPhones(
      String fieldName, TelephoneType type, Map<String, Object> map, VCard card) {
    getList(fieldName, map)
        .ifPresent(v -> v.forEach(number -> card.addTelephoneNumber(number, type)));
  }

  private void copyExtendedData(Map<String, Object> map, VCard card) {
    getString("manager", map).ifPresent(v -> card.setExtendedProperty("X-Manager", v));
    getString("spouseName", map).ifPresent(v -> card.setExtendedProperty("X-Spouse", v));
  }
}

/*
TODO Investigate:
imAddresses	String collection	The contact's instant messaging (IM) addresses.
createdDateTime	DateTimeOffset	The time the contact was created. The Timestamp type represents date and time information using ISO 8601 format and is always in UTC time. For example, midnight UTC on Jan 1, 2014 would look like this: '2014-01-01T00:00:00Z'
department	String	The contact's department.
officeLocation	String	The location of the contact's office.
initials	String	The contact's initials.
children	String collection	The names of the contact's children.
generation	String	The contact's generation.
yomiCompanyName	String	The phonetic Japanese company name of the contact.
yomiGivenName	String	The phonetic Japanese given name (first name) of the contact.
yomiSurname
fileAs	String	The name the contact is filed under.
lastModifiedDateTime	DateTimeOffset	The time the contact was modified. The Timestamp type represents date and time information using ISO 8601 format and is always in UTC time. For example, midnight UTC on Jan 1, 2014 would look like this: '2014-01-01T00:00:00Z'



Not added:
parentFolderId	String	The ID of the contact's parent folder.
id	String	The contact's unique identifier. Read-only.
changeKey	String	Identifies the version of the contact. Every time the contact is changed, ChangeKey changes as well. This allows Exchange to apply changes to the correct version of the object.
 */
