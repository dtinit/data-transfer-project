package org.datatransferproject.auth.amazon;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.datatransferproject.auth.OAuth2Config;
import org.datatransferproject.types.common.models.DataVertical;

import java.util.Map;
import java.util.Set;

import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

/**
 * OAuth2 configuration for Amazon Photos (Login with Amazon).
 */
public class AmazonOAuthConfig implements OAuth2Config {

  @Override
  public String getServiceName() {
    return "Amazon";
  }

  @Override
  public String getAuthUrl() {
    return "https://www.amazon.com/ap/oa";
  }

  @Override
  public String getTokenUrl() {
    return "https://api.amazon.com/auth/o2/token";
  }

  @Override
  public Map<DataVertical, Set<String>> getImportScopes() {
    return ImmutableMap.of(
        PHOTOS, ImmutableSet.of(
            "amazonphotos::images:create",
            "amazonphotos::albums:create",
            "amazonphotos::albums:update"),
        VIDEOS, ImmutableSet.of(
            "amazonphotos::videos:create",
            "amazonphotos::albums:create",
            "amazonphotos::albums:update"),
        // MEDIA is the unified vertical (photos + videos + albums), so it needs the union of the
        // image, video and album scopes.
        MEDIA, ImmutableSet.of(
            "amazonphotos::images:create",
            "amazonphotos::videos:create",
            "amazonphotos::albums:create",
            "amazonphotos::albums:update"));
  }

  @Override
  public Map<DataVertical, Set<String>> getExportScopes() {
    return ImmutableMap.of(
        PHOTOS, ImmutableSet.of(
            "amazonphotos::images:read",
            "amazonphotos::albums:read"),
        VIDEOS, ImmutableSet.of(
            "amazonphotos::videos:read",
            "amazonphotos::albums:read"),
        // MEDIA is the unified vertical (photos + videos + albums); declare the union so the auth
        // framework can build import/export generators symmetrically with PHOTOS/VIDEOS.
        MEDIA, ImmutableSet.of(
            "amazonphotos::images:read",
            "amazonphotos::videos:read",
            "amazonphotos::albums:read"));
  }
}
