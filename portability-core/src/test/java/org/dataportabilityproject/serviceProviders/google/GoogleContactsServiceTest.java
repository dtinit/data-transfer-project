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

package org.dataportabilityproject.serviceProviders.google;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.dataportabilityproject.serviceProviders.google.contacts.GoogleContactsService;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.people.v1.PeopleService;
import com.google.api.services.people.v1.model.EmailAddress;
import com.google.api.services.people.v1.model.Name;
import com.google.api.services.people.v1.model.Person;
import com.google.api.services.people.v1.model.PhoneNumber;
import com.google.gdata.util.common.base.Pair;
import ezvcard.VCard;
import ezvcard.property.Email;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class GoogleContactsServiceTest {
  private final Logger logger = LoggerFactory.getLogger(GoogleContactsServiceTest.class);

  @Test
  public void convertToVCardEmails() {
    int numEmails = 4;
    List<EmailAddress> googleEmails = new LinkedList<>();
    for (int i = 0; i < numEmails; i++) {
      googleEmails.add(new EmailAddress().setDisplayName("email" + i + "@gmail.com"));
    }

    List<Email> vcardEmails = GoogleContactsService.convertToVcardEmails(googleEmails);
  }
}
