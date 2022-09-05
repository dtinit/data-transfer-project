package org.datatransferproject.spi.api.auth;

import java.util.Set;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.types.common.models.DataVertical;

/** Manages {@link AuthServiceExtension}s registered in the system. */
public interface AuthServiceProviderRegistry {
  /**
   * Lookup the @code{AuthDataGenerator} corresponding to the serviceId, transferType and AuthMode
   * specified
   *
   * @param serviceId the AuthServiceExtension to use
   * @param transferDataType The transfer data type to authorize for
   * @param mode The authorization mode
   * @return An AuthDataGenerator from the specified AuthServiceExtension for the type and mode
   *     requested.
   */
  AuthDataGenerator getAuthDataGenerator(String serviceId, DataVertical transferDataType, AuthMode mode);

  /**
   * Returns the set of service ids that can import the given {@code transferDataType}.
   *
   * @param transferDataType the transfer data type
   */
  Set<String> getImportServices(DataVertical transferDataType);

  /**
   * Returns the set of service ids that can export the given {@code transferDataType}.
   *
   * @param transferDataType the transfer data type
   */
  Set<String> getExportServices(DataVertical transferDataType);

  /** Returns the set of data types that support both import and export.
   * @return*/
  Set<DataVertical> getTransferDataTypes();

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
