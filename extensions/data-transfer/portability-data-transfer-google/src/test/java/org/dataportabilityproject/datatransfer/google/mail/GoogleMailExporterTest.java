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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.Gmail.Users.Messages.Get;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.dataportabilityproject.datatransfer.google.common.GoogleCredentialFactory;
import org.dataportabilityproject.spi.transfer.provider.ExportResult;
import org.dataportabilityproject.spi.transfer.types.ContinuationData;
import org.dataportabilityproject.spi.transfer.types.ExportInformation;
import org.dataportabilityproject.spi.transfer.types.PaginationData;
import org.dataportabilityproject.spi.transfer.types.StringPaginationToken;
import org.dataportabilityproject.types.transfer.models.mail.MailContainerResource;
import org.dataportabilityproject.types.transfer.models.mail.MailMessageModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;

public class GoogleMailExporterTest {

  private static final UUID JOB_ID = UUID.randomUUID();

  private static final String NEXT_TOKEN = "next_token";

  private static final String MESSAGE_ID = "messageId";
  private static final String MESSAGE_RAW = "message contents";
  private static final List<String> MESSAGE_LABELS = ImmutableList.of("label1", "label2");
  private static final Message INITIAL_MESSAGE = new Message().setId(MESSAGE_ID);
  private static final Message FULL_MESSAGE = new Message().setId(MESSAGE_ID).setRaw(MESSAGE_RAW)
      .setLabelIds(MESSAGE_LABELS);

  private Users users;
  private Messages messages;
  private Messages.List messageListRequest;
  private ListMessagesResponse messageListResponse;
  private Get get;

  private Gmail gmail;

  private GoogleMailExporter googleMailExporter;

  @Before
  public void setup() throws IOException {
    users = mock(Users.class);
    messages = mock(Messages.class);
    messageListRequest = mock(Messages.List.class);
    get = mock(Get.class);
    gmail = mock(Gmail.class);

    GoogleCredentialFactory googleCredentialFactory = mock(GoogleCredentialFactory.class);
    googleMailExporter = new GoogleMailExporter(googleCredentialFactory, gmail);

    when(gmail.users()).thenReturn(users);
    when(users.messages()).thenReturn(messages);
    when(messages.list(Matchers.anyString())).thenReturn(messageListRequest);
    when(messageListRequest.setMaxResults(Matchers.anyLong())).thenReturn(messageListRequest);
    when(messages.get(Matchers.anyString(), Matchers.anyString())).thenReturn(get);
    when(get.setFormat(Matchers.anyString())).thenReturn(get);

    verifyZeroInteractions(googleCredentialFactory);
  }

  @Test
  public void exportMessagesFirstSet() throws IOException {
    setUpSingleMessageResponse();

    // Looking at first page, with at least one page after it
    messageListResponse.setNextPageToken(NEXT_TOKEN);

    // Run test
    ExportResult<MailContainerResource> result =
        googleMailExporter.export(JOB_ID, null, Optional.empty());

    // Check results
    // Verify correct methods were called
    InOrder inOrder = Mockito.inOrder(messages, messageListRequest, get);
    // First request
    inOrder.verify(messages).list(GoogleMailExporter.USER);
    inOrder.verify(messageListRequest).setMaxResults(GoogleMailExporter.PAGE_SIZE);
    verify(messageListRequest, never()).setPageToken(Matchers.anyString());
    // Second request
    inOrder.verify(messages).get(GoogleMailExporter.USER, MESSAGE_ID);
    inOrder.verify(get).setFormat("raw");
    inOrder.verify(get).execute();

    // Check pagination token
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken.getToken()).isEqualTo(NEXT_TOKEN);

    // Check messages
    Collection<MailMessageModel> actualMail = result.getExportedData().getMessages();
    assertThat(actualMail.stream().map(MailMessageModel::getRawString).collect(Collectors.toList()))
        .containsExactly(MESSAGE_RAW);
    assertThat(
        actualMail.stream().map(MailMessageModel::getContainerIds).collect(Collectors.toList()))
        .containsExactly(MESSAGE_LABELS);
  }

  @Test
  public void exportMessagesSubsequentSet() throws IOException {
    setUpSingleMessageResponse();

    // Looking at subsequent page, with no page after it
    PaginationData paginationData = new StringPaginationToken(NEXT_TOKEN);
    ExportInformation exportInformation = new ExportInformation(paginationData, null);
    messageListResponse.setNextPageToken(null);

    // Run test
    ExportResult<MailContainerResource> result =
        googleMailExporter.export(JOB_ID, null, Optional.of(exportInformation));

    // Check results
    // Verify correct calls were made (i.e., token was set before execution)
    InOrder inOrder = Mockito.inOrder(messageListRequest);
    inOrder.verify(messageListRequest).setPageToken(NEXT_TOKEN);
    inOrder.verify(messageListRequest).execute();

    // Check pagination token (should be null)
    ContinuationData continuationData = (ContinuationData) result.getContinuationData();
    StringPaginationToken paginationToken =
        (StringPaginationToken) continuationData.getPaginationData();
    assertThat(paginationToken).isNull();
  }

  /**
   * Sets up a response with a single message
   */
  private void setUpSingleMessageResponse() throws IOException {
    messageListResponse = new ListMessagesResponse()
        .setMessages(Collections.singletonList(INITIAL_MESSAGE));
    when(messageListRequest.execute()).thenReturn(messageListResponse);
    when(get.execute()).thenReturn(FULL_MESSAGE);
  }
}
