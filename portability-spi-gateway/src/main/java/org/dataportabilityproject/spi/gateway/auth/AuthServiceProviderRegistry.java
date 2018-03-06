package org.dataportabilityproject.spi.gateway.auth;

import java.util.Set;

/** Manages {@link AuthServiceProvider}s registered in the system. */
public interface AuthServiceProviderRegistry {
  /**
   * Lookup the @code{AuthDataGenerator} corresponding to the serviceId, transferType and AuthMode
   * specified
   *
   * @param serviceId the AuthServiceProvider to use
   * @param transferDataType The TransferdataType to authorize for
   * @param mode The authorization mode
   * @return An AuthDataGenerator from the specified AuthServiceProvider for the type and mode
   *     requested.
   */
  AuthDataGenerator getAuthDataGenerator(String serviceId, String transferDataType, AuthMode mode);

  /**
   * Returns the set of service ids that can transfered for the given {@code transferDataType}.
   *
   * @param transferDataType the transfer data type
   */
  Set<String> getServices(String transferDataType);

  /** Returns the set of data types that support both import and export. */
  Set<String> getTransferDataTypes();

  /**
   * The AuthorizationMode to use for lookups. IMPORT specifies an authorization that allows you to
   * import a type (implying read-write permissions) EXPORT specifies an authorization that allows
   * you to export a type (implying read-only permissions)
   */
  enum AuthMode {
    IMPORT,
    EXPORT
  }
}
