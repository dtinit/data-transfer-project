package org.datatransferproject.auth.amazon;

import org.datatransferproject.auth.OAuth2ServiceExtension;

public class AmazonAuthServiceExtension extends OAuth2ServiceExtension {
  public AmazonAuthServiceExtension() {
    super(new AmazonOAuthConfig());
  }
}
