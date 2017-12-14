package org.dataportabilityproject.webapp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import org.dataportabilityproject.shared.settings.ApiSettings;

/**
 * A class that contains all flags exlusive to the API server.
 */
public class PortabilityApiFlags {
  private static PortabilityApiFlags INSTANCE = null;
  private final ApiSettings apiSettings;

  private PortabilityApiFlags(ApiSettings apiSettings) {
    this.apiSettings = apiSettings;
  }

  /**
   * Initialize PortabilityApiFlags global configuration parameters from provided command line.
   */
  public static void parse() {
    if (INSTANCE != null) {
      throw new IllegalStateException("Trying to initialize flags a second time");
    }

    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    try {
      InputStream in =
          PortabilityApiFlags.class.getClassLoader().getResourceAsStream("settings/api.yaml");
      ApiSettings apiSettings = mapper.readValue(in, ApiSettings.class);
      INSTANCE = new PortabilityApiFlags(apiSettings);
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem parsing api settings", e);
    }
  }

  public static String baseUrl() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'baseUrl' before flags have been initialized");
    return INSTANCE.apiSettings.getBaseUrl();
  }

  public static String baseApiUrl() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'baseApiUrl' before flags have been initialized");
    return INSTANCE.apiSettings.getBaseApiUrl();
  }
}
