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
            "photos::images:create",
            "photos::albums:create",
            "photos::albums:update");
  }

  @Test
  void importScopesForVideos() {
    assertThat(config.getImportScopes().get(VIDEOS))
        .containsExactly(
            "photos::videos:create",
            "photos::albums:create",
            "photos::albums:update");
  }

  @Test
  void exportScopesForPhotos() {
    assertThat(config.getExportScopes().get(PHOTOS))
        .containsExactly("photos::images:read", "photos::albums:read");
  }

  @Test
  void exportScopesForVideos() {
    assertThat(config.getExportScopes().get(VIDEOS))
        .containsExactly("photos::videos:read", "photos::albums:read");
  }
}
