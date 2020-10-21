package org.datatransferproject.datatransfer.backblaze.common;

import java.io.IOException;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.datatransfer.backblaze.exception.BackblazeCredentialsException;
import org.datatransferproject.types.transfer.auth.TokenSecretAuthData;

public class BackblazeDataTransferClientFactory {
  private BackblazeDataTransferClient b2Client;

  public BackblazeDataTransferClient getOrCreateB2Client(
      Monitor monitor, TokenSecretAuthData authData)
      throws BackblazeCredentialsException, IOException {
    if (b2Client == null) {
      BackblazeDataTransferClient backblazeDataTransferClient =
          new BackblazeDataTransferClient(monitor);
      backblazeDataTransferClient.init(authData.getToken(), authData.getSecret());
      b2Client = backblazeDataTransferClient;
    }
    return b2Client;
  }
}
