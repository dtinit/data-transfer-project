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

package org.datatransferproject.datatransfer.google.mail;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.google.common.GoogleCredentialFactory;
import org.datatransferproject.spi.transfer.idempotentexecutor.IdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.ImportResult;
import org.datatransferproject.test.types.FakeIdempotentImportExecutor;
import org.datatransferproject.types.common.models.mail.MailContainerResource;
import org.datatransferproject.types.common.models.mail.MailMessageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class GoogleMailImporterTest {

  private static final UUID JOB_ID = UUID.randomUUID();

  private static final String MESSAGE_RAW = "message content";
  private static final String LABEL1 = "label1";
  private static final String LABEL2 = "label2";
  private static final List<String> MESSAGE_LABELS = ImmutableList.of(LABEL1, LABEL2);
  private static final MailMessageModel MESSAGE_MODEL =
      new MailMessageModel(MESSAGE_RAW, MESSAGE_LABELS);

  @Mock
  private Gmail gmail;
  @Mock
  private Users users;
  @Mock
  private Messages messages;
  @Mock
  private Insert insert;
  @Mock
  private Labels labels;
  @Mock
  private Labels.List labelsList;
  @Mock
  private Labels.Create labelsCreate;
  @Mock
  private GoogleCredentialFactory googleCredentialFactory;

  private ListLabelsResponse labelsListResponse;
  private GoogleMailImporter googleMailImporter;
  private IdempotentImportExecutor executor;

  @BeforeEach
  public void setUp() throws IOException {
    Label label = new Label();
    label.setId(LABEL1);
    label.setName(LABEL1);
    labelsListResponse = new ListLabelsResponse().setLabels(Collections.singletonList(label));

    Monitor monitor = new Monitor() {
    };
    googleMailImporter = new GoogleMailImporter(googleCredentialFactory, gmail, monitor);
    executor = new FakeIdempotentImportExecutor();

    when(gmail.users()).thenReturn(users);
    when(users.messages()).thenReturn(messages);
    when(messages.insert(anyString(), any(Message.class))).thenReturn(insert);
    when(insert.execute()).thenReturn(new Message().setId("fooBar"));
    when(users.labels()).thenReturn(labels);
    when(labels.list(anyString())).thenReturn(labelsList);
    when(labelsList.execute()).thenReturn(labelsListResponse);
    when(labels.create(anyString(), any(Label.class))).thenReturn(labelsCreate);
    when(labelsCreate.execute()).thenReturn(label);

    verifyNoInteractions(googleCredentialFactory);
  }

  @Test
  public void importMessage() throws Exception {
    MailContainerResource resource =
        new MailContainerResource(null, Collections.singletonList(MESSAGE_MODEL));

    ImportResult result = googleMailImporter.importItem(JOB_ID, executor, null, resource);

    // Getting list of labels from Google
    verify(labelsList, atLeastOnce()).execute();
    // Importing message
    ArgumentCaptor<Message> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(messages).insert(eq(GoogleMailImporter.USER), messageArgumentCaptor.capture());
    assertThat(messageArgumentCaptor.getValue().getRaw()).isEqualTo(MESSAGE_RAW);
    // TODO(olsona): test labels
  }
}
