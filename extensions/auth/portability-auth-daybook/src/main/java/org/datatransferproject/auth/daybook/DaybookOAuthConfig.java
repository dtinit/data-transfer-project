package org.datatransferproject.auth.daybook;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;

/**
 * Class that provides Daybook-specific information for OAuth2
 */
public class DaybookOAuthConfig implements OAuth2Config {

  @Override
  public String getServiceName() {
    return "Daybook";
  }

  @Override
  public String getAuthUrl() {
    return "https://voice.daybook.app";
  }

  @Override
  public String getTokenUrl() {
    return "https://auth.session.daybooklabs.com/api/auth";
  }

  @Override
  public Map<String, Set<String>> getExportScopes() {
    return ImmutableMap.of("PHOTOS", ImmutableSet.of("db.read"));
  }

  @Override
  public Map<String, Set<String>> getImportScopes() {
    return ImmutableMap.of("PHOTOS", ImmutableSet.of("db.write"));
  }
}