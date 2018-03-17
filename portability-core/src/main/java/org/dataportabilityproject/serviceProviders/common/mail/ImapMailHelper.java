/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.common.mail;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.io.BaseEncoding;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;
import javax.annotation.Nullable;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.dataModels.mail.MailContainerModel;
import org.dataportabilityproject.dataModels.mail.MailMessageModel;
import org.dataportabilityproject.dataModels.mail.MailModelWrapper;
import org.dataportabilityproject.shared.IdOnlyResource;
import org.dataportabilityproject.shared.IntPaginationToken;

/** Interfaces with an imap server, authenticating using name and password */
public class ImapMailHelper {

  public static final String PROTOCOL = "imaps";

  private static final BaseEncoding BASE_ENCODER = BaseEncoding.base64Url();
  // TODO: Configure MAX_RESULTS_PER_REQUEST to a reasonable number of messages to process in memory
  // Max results to fetch on each request for more mail messages
  private static final int MAX_RESULTS_PER_REQUEST = 10;

  /** Whether to enable imap debugging. */
  private final boolean debug;

  public ImapMailHelper() {
    this(false);
  }

  public ImapMailHelper(boolean debug) {
    this.debug = debug;
  }

  private static int getStart(PaginationInformation paginationInformation) {
    int start = 1;
    if (paginationInformation != null) {
      start = Math.max(((IntPaginationToken) paginationInformation).getStart(), start);
    }
    return start;
  }

  // TODO: Move to hierarchical model

  private static int getEnd(int start, int totalNumMessages) {
    return Math.min(((start + MAX_RESULTS_PER_REQUEST) - 1), totalNumMessages);
  }

  private static Properties createProperties(String host, String user, boolean debug) {
    Properties props = new Properties();
    props.put("mail.imap.ssl.enable", "true"); // required for Gmail

    // disable other auth
    props.put("mail.imap.auth.login.disable", "false");
    props.put("mail.imap.auth.plain.disable", "false");

    // timeouts
    props.put("mail.imaps.connectiontimeout", "10000");
    props.put("mail.imaps.timeout", "10000");

    props.setProperty("mail.store.protocol", PROTOCOL);
    props.setProperty("mail.imap.user", user);
    props.setProperty("mail.imap.host", host);

    if (debug) {
      props.put("mail.debug", "true");
      props.put("mail.debug.auth", "true");
    }

    // extra code required for reading messages during IMAP-start
    props.setProperty("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.setProperty("mail.imaps.socketFactory.fallback", "false");
    props.setProperty("mail.imaps.port", "993");
    props.setProperty("mail.imaps.socketFactory.port", "993");
    // extra codes required for reading OUTLOOK mails during IMAP-end

    // XOAUTH unsupported, consider uncommenting if needed
    // props.put("mail.imap.sasl.enable", "true");
    // props.put("mail.imap.sasl.mechanisms", "XOAUTH2");
    // props.put("mail.imaps.sasl.mechanisms.oauth2.oauthToken", token);

    return props;
  }

  /** Creates a raw representation of the given email {@code message} */
  private static String createRawMessage(Message message) throws MessagingException, IOException {
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    message.writeTo(outstream);
    return BASE_ENCODER.encode(outstream.toByteArray());
  }

  // TODO: Replace with logging framework
  private static void log(String fmt, Object... args) {
    System.out.println(String.format("ImapMailHelper: " + fmt, args));
  }

  public MailModelWrapper getFolderContents(
      String host,
      String account,
      String password,
      @Nullable String folderName,
      PaginationInformation paginationInformation)
      throws MessagingException, IOException {
    Properties props = createProperties(host, account, debug);

    Session session = Session.getInstance(props);
    Store store = session.getStore(PROTOCOL);

    log("getFolderContents connecting to store");
    try {
      store.connect(host, account, password);
    } catch (MessagingException e) {
      log("Exception connecting to: %s, error: %s", host, e.getMessage());
      throw e;
    }

    // If no folder specified, return the default folder
    if (folderName == null) {
      log("getMessages from default folder");
      Folder defaultFolder;
      try {
        defaultFolder = store.getDefaultFolder();
      } catch (MessagingException e) {
        log("Exception getting default folder: %s", e.getMessage());
        throw e;
      }
      return getMessages(host, account, password, defaultFolder, false, paginationInformation);
    }

    log("getMessages for specific folder: %s", folderName);
    // Fetch the contents of the specified folder
    Folder folder = store.getFolder(folderName);
    if (folder == null || !folder.exists()) {
      throw new IOException("Folder not found, name: " + folderName);
    }

    return getMessages(host, account, password, folder, true, paginationInformation);
  }

  /** Get all messages in an account. */
  private MailModelWrapper getMessages(
      String host,
      String account,
      String password,
      Folder parentFolder,
      boolean fetchMessages,
      PaginationInformation paginationInformation)
      throws MessagingException, IOException {

    int foldersSize = 0;
    // Find containers to and be imported
    ImmutableCollection.Builder<MailContainerModel> folders = ImmutableList.builder();
    ImmutableCollection.Builder<Resource> folderIds = ImmutableList.builder();
    log("Calling list for folder: %s", parentFolder.getName());
    Folder[] subFolders = parentFolder.list();
    log("Folder: %s, subFolders: %d", parentFolder.getName(), subFolders.length);
    for (Folder folder : subFolders) {
      // This will tell the framework to create this folder on import
      folders.add(new MailContainerModel(folder.getName(), folder.getFullName()));
      // Content for these resources will be 'fetched' by the framework
      folderIds.add(new IdOnlyResource(folder.getName()));
      foldersSize++;
    }
    log("foldersSize: %d", foldersSize);

    // Get messages in the folder
    ImmutableCollection.Builder<MailMessageModel> resources = ImmutableList.builder();
    log("fetchMessages: %b", fetchMessages);
    PaginationInformation nextPaginationInformation = null;
    if (fetchMessages) {
      parentFolder.open(Folder.READ_ONLY);
      int start = getStart(paginationInformation);
      int end = getEnd(start, parentFolder.getMessageCount());
      if (end < parentFolder.getMessageCount()) {
        // Indicates page to be fetched on next request
        nextPaginationInformation =
            new IntPaginationToken(end + 1 /* the start index for next iteration */);
      }
      log(
          "Fetching messages for foder: %s, start: %d, end: %d",
          parentFolder.getFullName(), start, end);
      Message[] messages = parentFolder.getMessages(start, end);
      log(
          "Fetched message for folder: %s, messages: %s",
          parentFolder.getFullName(), messages.length);
      for (Message message : messages) {
        log("Message, contentType: %s ,size: %s", message.getContentType(), message.getSize());
        ImmutableList<String> folderId = ImmutableList.of(parentFolder.getName());
        resources.add(new MailMessageModel(createRawMessage(message), folderId));
      }
      parentFolder.close(false);
    }

    // TODO: add pagination below
    return new MailModelWrapper(
        folders.build(),
        resources.build(),
        new ContinuationInformation(folderIds.build(), nextPaginationInformation));
  }
}
