package org.dataportabilityproject.serviceProviders.common.mail;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import org.dataportabilityproject.dataModels.mail.MailMessageModel;

/**
 * Interfaces with an imap server, authenticating using name and password
 */
public class ImapMailHelper {

  public static final String PROTOCOL = "imaps";
  /** Whether to enable imap debugging. */
  private final boolean debug;

  public ImapMailHelper() {
    this(false);
  }

  public ImapMailHelper(boolean debug) {
    this.debug = debug;
  }

  public Collection<MailMessageModel> getAllMessages(String host, String account, String password)
      throws MessagingException, IOException {

    Properties props = createProperties(host, account, debug);

    Session session = Session.getInstance(props);
    Store store = session.getStore(PROTOCOL);
    
    try {
      store.connect(host, account, password);
    } catch (MessagingException e) {
      System.out.println("Exception connecting to: " + host + ", error: " + e.getMessage());
      throw e;
    }

    Folder defaultFolder;
    try {
      defaultFolder = store.getDefaultFolder();
    } catch (MessagingException e) {
      System.out.println("Exception getting default folder: " + e.getMessage());
      throw e;
    }

    // Process each subfolder
    Folder[] folders = defaultFolder.list();
    ImmutableCollection.Builder<MailMessageModel> results = ImmutableList.builder();
    for (Folder folder : folders) {
      results.add(new MailMessageModel(folder.getName()));
      folder.open(Folder.READ_ONLY);
      // TODO: Process nested folders
      Message[] messages = folder.getMessages(1, folder.getMessageCount());
      for (Message message : messages) {
        results.add(new MailMessageModel(createRawMessage(message)));
      }
      folder.close(false);
    }
    store.close();
    return results.build();
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
  
    //extra code required for reading messages during IMAP-start
    props.setProperty("mail.imaps.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    props.setProperty("mail.imaps.socketFactory.fallback", "false");
    props.setProperty("mail.imaps.port", "993");
    props.setProperty("mail.imaps.socketFactory.port", "993");
      //extra codes required for reading OUTLOOK mails during IMAP-end

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
    return outstream.toString(); // TODO: This assumes platform encoding of the string
  }
}
