package org.dataportabilityproject.serviceProviders.google.mail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.mail.MailMessageModel;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;

public final class GoogleMailService
    implements Exporter<MailMessageModel>, Importer<MailMessageModel> {
  private static final String USER = "me";
  private static final String LABEL = "WT-migrated";
  private final Gmail gmail;

  public GoogleMailService(Credential credential) {
    this.gmail = new Gmail.Builder(
            GoogleStaticObjects.getHttpTransport(),
            GoogleStaticObjects.JSON_FACTORY,
            credential)
        .setApplicationName(GoogleStaticObjects.APP_NAME)
        .build();
  }

  @Override
  public void importItem(MailMessageModel model) throws IOException {
    String labelId = getMigratedLabelId();
    Message message = new Message()
        .setRaw(model.getRawString())
        .setLabelIds(ImmutableList.of(labelId));
    gmail.users().messages().insert(USER, message).execute();
  }

  @Override
  public Collection<MailMessageModel> export() throws IOException {
    ListMessagesResponse listMessages = gmail.users().messages()
        .list(USER).setMaxResults(10L).execute();
    ImmutableList.Builder<MailMessageModel> results = ImmutableList.builder();
    // TODO: this is a good indication we need to swap the interface
    // as we can't store all the mail messagess in memory at once.
    for (Message listMessage : listMessages.getMessages()) {
      Message getResponse = gmail.users().messages()
          .get(USER, listMessage.getId()).setFormat("raw").execute();
      // TODO: note this doesn't transfer things like labels
      results.add(new MailMessageModel(getResponse.getRaw()));
    }

    return results.build();
  }

  private String getMigratedLabelId() throws IOException {
    ListLabelsResponse response = gmail.users().labels().list(USER).execute();
    for (Label label : response.getLabels()) {
      if (label.getName().equals(LABEL)) {
        return label.getId();
      }
    }

    Label newLabel = new Label()
        .setName(LABEL)
        .setLabelListVisibility("labelShow")
        .setMessageListVisibility("show");
    return gmail.users().labels().create(USER, newLabel).execute().getId();
  }
}
