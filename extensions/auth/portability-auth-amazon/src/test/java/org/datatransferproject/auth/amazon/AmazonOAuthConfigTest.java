package org.datatransferproject.auth.amazon;

import static com.google.common.truth.Truth.assertThat;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AmazonOAuthConfigTest {

  private AmazonOAuthConfig config;

  @BeforeEach
  void setUp() {
    config = new AmazonOAuthConfig();
  }

  @Test
  void serviceName() {
    assertThat(config.getServiceName()).isEqualTo("Amazon");
  }

  @Test
  void authUrlIsLwaEndpoint() {
    assertThat(config.getAuthUrl()).isEqualTo("https://www.amazon.com/ap/oa");
  }

  @Test
  void tokenUrlIsLwaEndpoint() {
    assertThat(config.getTokenUrl()).isEqualTo("https://api.amazon.com/auth/o2/token");
  }

  @Test
  void importScopesForPhotos() {
    assertThat(config.getImportScopes().get(PHOTOS))
        .containsExactly(
            "amazonphotos::images:create",
            "amazonphotos::albums:create",
            "amazonphotos::albums:update");
  }

  @Test
  void importScopesForVideos() {
    assertThat(config.getImportScopes().get(VIDEOS))
        .containsExactly(
            "amazonphotos::videos:create",
            "amazonphotos::albums:create",
            "amazonphotos::albums:update");
  }

  @Test
  void exportScopesEmpty() {
    assertThat(config.getExportScopes()).isEmpty();
  }
}
