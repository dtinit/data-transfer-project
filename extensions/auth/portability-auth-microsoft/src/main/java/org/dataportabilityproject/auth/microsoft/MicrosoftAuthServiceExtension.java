package org.dataportabilityproject.auth.microsoft;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import okhttp3.OkHttpClient;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.AppCredentialStore;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;
import org.dataportabilityproject.spi.gateway.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.types.transfer.auth.AppCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** */
public class MicrosoftAuthServiceExtension implements AuthServiceExtension {
  private static final ImmutableList<String> DEFAULT_AUTH_GENERATOR_SERVICES =
      ImmutableList.of("contacts", "calendar");
  private static final Logger logger = LoggerFactory.getLogger(MicrosoftAuthServiceExtension.class);

  private volatile MicrosoftAuthDataGenerator authDataGenerator;

  public MicrosoftAuthServiceExtension() {}

  public String getServiceId() {
    return "microsoft";
  }

  public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
    // TODO Create auth data generator for a given mode, usually means a different scope
    if (DEFAULT_AUTH_GENERATOR_SERVICES.contains(transferDataType)) {
      return authDataGenerator;
    }
    throw new UnsupportedOperationException("transferDataType does not have an AuthDataGenerator");
  }

  @Override
  public List<String> getImportTypes() {
    return DEFAULT_AUTH_GENERATOR_SERVICES;
  }

  @Override
  public List<String> getExportTypes() {
    return DEFAULT_AUTH_GENERATOR_SERVICES;
  }

  @Override
  public void initialize(ExtensionContext context) {
    makeAuthDataGenerator(context);
  }

  private synchronized MicrosoftAuthDataGenerator makeAuthDataGenerator(ExtensionContext context) {
    if (authDataGenerator == null) {
      AppCredentials credentials;
      try {
        credentials = context.getService(AppCredentialStore.class)
                .getAppCredentials("MICROSOFT_KEY", "MICROSOFT_SECRET");
      } catch (IOException e) {
        logger.warn("Problem getting AppCredentials: {}", e);
        return null;
      }

      authDataGenerator =
          new MicrosoftAuthDataGenerator(
              // TODO: Figure out the appropriate keys and objects
              context.getConfiguration("REDIRECT_PATH", (String) null),
              credentials,
              (OkHttpClient) null,
              context.getTypeManager().getMapper());
    }
    return authDataGenerator;
  }
}
