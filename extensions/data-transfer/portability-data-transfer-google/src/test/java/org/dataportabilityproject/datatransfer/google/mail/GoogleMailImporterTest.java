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

package org.dataportabilityproject.datatransfer.google.mail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users;
import com.google.api.services.gmail.Gmail.Users.Labels;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.Gmail.Users.Messages.Insert;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.dataportabilityproject.cloud.local.LocalJobStore;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.provider.ImportResult;
import org.dataportabilityproject.types.transfer.models.mail.MailContainerResource;
import org.dataportabilityproject.types.transfer.models.mail.MailMessageModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

public class GoogleMailImporterTest {

  private static final UUID JOB_ID = UUID.randomUUID();

  private static final String MESSAGE_RAW = "message content";
  private static final String LABEL1 = "label1";
  private static final String LABEL2 = "label2";
  private static final List<String> MESSAGE_LABELS = ImmutableList.of(LABEL1, LABEL2);
  private static final MailMessageModel MESSAGE_MODEL = new MailMessageModel(MESSAGE_RAW,
      MESSAGE_LABELS);

  private JobStore jobStore;
  private Gmail gmail;
  private Users users;
  private Messages messages;
  private Insert insert;
  private Labels labels;
  private Labels.List labelsList;
  private ListLabelsResponse labelsListResponse;

  private GoogleMailImporter googleMailImporter;

  @Before
  public void setUp() throws IOException {
    gmail = mock(Gmail.class);
    users = mock(Users.class);
    messages = mock(Messages.class);
    insert = mock(Insert.class);
    labels = mock(Labels.class);
    labelsList = mock(Labels.List.class);

    Label label = new Label();
    label.setId(LABEL1);
    labelsListResponse = new ListLabelsResponse().setLabels(Collections.singletonList(label));

    GoogleCredentialFactory googleCredentialFactory = mock(GoogleCredentialFactory.class);
    jobStore = new LocalJobStore();
    googleMailImporter = new GoogleMailImporter(googleCredentialFactory, jobStore, gmail);

    when(gmail.users()).thenReturn(users);
    when(users.messages()).thenReturn(messages);
    when(messages.insert(Matchers.anyString(), Matchers.any(Message.class))).thenReturn(insert);
    when(users.labels()).thenReturn(labels);
    when(labels.list(Matchers.anyString())).thenReturn(labelsList);
    when(labelsList.execute()).thenReturn(labelsListResponse);

    verifyZeroInteractions(googleCredentialFactory);
  }

  @Test
  public void importMessage() {
    MailContainerResource resource = new MailContainerResource(null,
        Collections.singletonList(MESSAGE_MODEL));

    ImportResult result = googleMailImporter.importItem(JOB_ID, null, resource);
  }
}
