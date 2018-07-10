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

import static org.datatransferproject.transfer.microsoft.transformer.common.TransformerHelper.safeSet;

import ezvcard.VCard;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.Email;
import ezvcard.property.Expertise;
import ezvcard.property.Organization;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.TransformerContext;

/**
 * Maps from a VCard to a Microsoft Graph contacts resource as defined by
 * https://developer.microsoft.com/en-us/graph/docs/api-reference/v1.0/resources/contact.
 */
public class ToGraphContactTransformer
    implements BiFunction<VCard, TransformerContext, Map<String, Object>> {

  @Override
  public Map<String, Object> apply(VCard card, TransformerContext context) {
    Map<String, Object> contact = new LinkedHashMap<>();
    copyNames(card, contact);
    copyPersonData(card, contact, context);
    copyBusinessData(card, contact);
    copyExtendedData(card, contact);
    return contact;
  }

  private void copyNames(VCard card, Map<String, Object> contact) {
    StructuredName structuredName = card.getStructuredName();
    if (structuredName != null) {
      safeSet("givenName", structuredName.getGiven(), contact);
      safeSet("surname", structuredName.getFamily(), contact);
      // MS contacts only allows one middle name - take the first one
      if (!structuredName.getAdditionalNames().isEmpty()) {
        safeSet("middleName", structuredName.getAdditionalNames().get(0), contact);
      }
      // MS contacts only allows one prefix - take the first one
      if (!structuredName.getPrefixes().isEmpty()) {
        safeSet("title", structuredName.getPrefixes().get(0), contact);
      }
    }
    safeSet("displayName", card.getFormattedName(), contact);
    safeSet("nickName", card.getNickname(), contact);
  }

  private void copyPersonData(VCard card, Map<String, Object> contact, TransformerContext context) {
    // Addresses are lossy: there is no distinction in VCard between business and home addresses
    if (!card.getAddresses().isEmpty()) {
      safeSet(
          "homeAddress",
          context.transform(LinkedHashMap.class, card.getAddresses().get(0)),
          contact);
    }

    card.getEmails().stream().filter(v -> v.getValue() != null).forEach(v -> addEmail(v, contact));

    card.getTelephoneNumbers()
        .stream()
        .filter(t -> t.getText() != null)
        .forEach(
            telephone -> {
              for (TelephoneType telephoneType : telephone.getTypes()) {
                if (TelephoneType.CELL.equals(telephoneType)) {
                  // this could overwrite some numbers since MS contacts only have one mobile
                  contact.put("mobilePhone", telephone.getText());
                } else if (TelephoneType.WORK.equals(telephoneType)) {
                  addPhone("businessPhones", telephone, contact);
                } else {
                  addPhone("homePhones", telephone, contact);
                }
              }
            });

    if (card.getBirthday() != null) {
      safeSet("birthday", card.getBirthday().getText(), contact);
    }
  }

  private void copyBusinessData(VCard card, Map<String, Object> contact) {
    for (Title title : card.getTitles()) {
      if (title.getValue() == null) {
        continue;
      }
      // This can loose data but there are only two title types in MS contacts
      if (title.getType() != null && title.getType().equalsIgnoreCase("home")) {
        safeSet("title", card.getTitles(), contact);
      } else {
        safeSet("jobTitle", card.getTitles(), contact);
      }
    }
    // This can loose data but there is only one company name in MS contacts
    for (Organization organization : card.getOrganizations()) {
      for (String orgName : organization.getValues()) {
        contact.put("companyName", orgName);
      }
    }

    for (Expertise expertise : card.getExpertise()) {
      if (expertise.getValue() == null) {
        continue;
      }
      contact.put("profession", expertise.getValue()); // only set first one
      break;
    }
  }

  private void copyExtendedData(VCard card, Map<String, Object> contact) {
    safeSet("manager", card.getExtendedProperty("X-Manager"), contact);
    safeSet("spouseName", card.getExtendedProperty("X-Spouse"), contact);
  }

  @SuppressWarnings("unchecked")
  private void addPhone(String key, Telephone telephone, Map<String, Object> map) {
    List<String> collection = (List<String>) map.computeIfAbsent(key, k -> new ArrayList<>());
    collection.add(telephone.getText());
  }

  @SuppressWarnings("unchecked")
  private void addEmail(Email email, Map<String, Object> map) {
    List<Map<String, String>> collection =
        (List<Map<String, String>>) map.computeIfAbsent("emailAddresses", k -> new ArrayList<>());
    Map<String, String> emailMap = new LinkedHashMap<>();
    emailMap.put("address", email.getValue());
    collection.add(emailMap);
  }
}
