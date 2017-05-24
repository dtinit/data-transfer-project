package org.dataportabilityproject.serviceProviders.microsoft.mail;

import java.io.IOException;
import java.util.Collection;
import javax.mail.MessagingException;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.mail.MailMessageModel;
import org.dataportabilityproject.dataModels.mail.MailModelWrapper;
import org.dataportabilityproject.serviceProviders.common.mail.ImapMailHelper;

public class MicrosoftMailService implements Exporter<MailModelWrapper>, Importer<MailModelWrapper> {

  public static final String HOST = "outlook.office365.com";
  public static final String PROTOCOL = "imap";
  private final String account;
  private final String password;

  public MicrosoftMailService(String account, String password) {
    this.account = account;
    this.password = password;
  }

  @Override
  public void importItem(MailModelWrapper object) throws IOException {
    System.out.println("Microsoft Mail Service import not implemented");
  }

  @Override
  public MailModelWrapper export(ExportInformation exportInformation) throws IOException {
    try {
      ImapMailHelper imapService = new ImapMailHelper();
      Collection<MailMessageModel> messages = imapService.getAllMessages(HOST, account, password);
      System.out.println("Exported messages: " + messages.size());
      return new MailModelWrapper(messages, null);
    } catch (MessagingException e) {
      e.printStackTrace();
      throw new IOException(e);
    }
  }
}
