package org.datatransferproject.auth.synology;

import org.datatransferproject.auth.OAuth2ServiceExtension;

public class SynologyAuthServiceExtension extends OAuth2ServiceExtension {
  public SynologyAuthServiceExtension() {
    super(new SynologyOAuthConfig());
  }
}
