package org.dataportabilityproject.serviceProviders.google.mail;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.Gmail.Users.Messages;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.PaginationInformation;
import org.dataportabilityproject.dataModels.mail.MailMessageModel;
import org.dataportabilityproject.dataModels.mail.MailModelWrapper;
import org.dataportabilityproject.serviceProviders.google.GoogleStaticObjects;
import org.dataportabilityproject.shared.StringPaginationToken;

public final class GoogleMailService
    implements Exporter<MailModelWrapper>, Importer<MailModelWrapper> {
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
  public void importItem(MailModelWrapper model) throws IOException {
    for (MailMessageModel message : model.getMessages()) {
      String labelId = getMigratedLabelId();
      Message newMessage = new Message()
          .setRaw(message.getRawString())
          .setLabelIds(ImmutableList.of(labelId));
      gmail.users().messages().insert(USER, newMessage).execute();
    }
  }

  @Override
  public MailModelWrapper export(ExportInformation exportInformation) throws IOException {
    Messages.List request = gmail.users().messages()
        .list(USER).setMaxResults(10L);

    if (exportInformation.getPaginationInformation().isPresent()) {
      request.setPageToken(
          ((StringPaginationToken)exportInformation.getPaginationInformation().get()).getId());
    }

    ListMessagesResponse response = request.execute();

    List<MailMessageModel> results = new ArrayList<>(response.getMessages().size());
    // TODO: this is a good indication we need to swap the interface
    // as we can't store all the mail messagess in memory at once.
    for (Message listMessage : response.getMessages()) {
      Message getResponse = gmail.users().messages()
          .get(USER, listMessage.getId()).setFormat("raw").execute();
      // TODO: note this doesn't transfer things like labels
      results.add(new MailMessageModel(getResponse.getRaw()));
    }

    PaginationInformation pageInfo = null;
    if (response.getNextPageToken() != null) {
      pageInfo = new StringPaginationToken(response.getNextPageToken());
    }

    return new MailModelWrapper(results, new ContinuationInformation(null, pageInfo));
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
