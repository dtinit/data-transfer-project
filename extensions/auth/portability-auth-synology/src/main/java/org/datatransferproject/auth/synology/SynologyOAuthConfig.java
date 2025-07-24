package org.datatransferproject.auth.synology;

import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.types.common.models.DataVertical;

public class SynologyOAuthConfig implements OAuth2Config {
  private static String AUTH_HOST = "https://identity.synology.com";
  private static String USER_INFO_SCOPE = "userinfo";
  private static String OFFLINE_ACCESS_SCOPE = "offline_access";

  @Override
  public String getServiceName() {
    return "Synology";
  }

  @Override
  public String getAuthUrl() {
    return AUTH_HOST + "/oauth2/auth";
  }

  @Override
  public String getTokenUrl() {
    return AUTH_HOST + "/oauth2/token";
  }

  @Override
  public Map<DataVertical, Set<String>> getExportScopes() {
    return ImmutableMap.<DataVertical, Set<String>>builder()
        .put(PHOTOS, ImmutableSet.of(USER_INFO_SCOPE, OFFLINE_ACCESS_SCOPE))
        .put(VIDEOS, ImmutableSet.of(USER_INFO_SCOPE, OFFLINE_ACCESS_SCOPE))
        .put(MEDIA, ImmutableSet.of(USER_INFO_SCOPE, OFFLINE_ACCESS_SCOPE))
        .build();
  }

  @Override
  public Map<DataVertical, Set<String>> getImportScopes() {
    return ImmutableMap.<DataVertical, Set<String>>builder()
        .put(PHOTOS, ImmutableSet.of(USER_INFO_SCOPE, OFFLINE_ACCESS_SCOPE))
        .put(VIDEOS, ImmutableSet.of(USER_INFO_SCOPE, OFFLINE_ACCESS_SCOPE))
        .put(MEDIA, ImmutableSet.of(USER_INFO_SCOPE, OFFLINE_ACCESS_SCOPE))
        .build();
  }
}
