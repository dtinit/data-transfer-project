package org.dataportabilityproject.shared;

/**
 * Global config for portability code specific for each environment.
 * TODO: Create configuration flags
 */
public final class Config {

  /**
   * Supported deployment environments.
   */
  public enum Environment {
    LOCAL, TEST, QA, PROD;
  }

  // Indicates whether the app is in prod
  private static final Environment ENV = Environment.PROD;

  /**
   * Base url for all calls within the application.
   */
  public static final String BASE_URL =
      (ENV == Environment.PROD) ? "http://35.193.13.205" : "http://localhost:3000";

  /**
   * Base url for direct to api calls within the application.
   */
  public static final String BASE_API_URL =
      (ENV == Environment.PROD) ? "http://35.193.13.205" : "http://localhost:8080";
}
