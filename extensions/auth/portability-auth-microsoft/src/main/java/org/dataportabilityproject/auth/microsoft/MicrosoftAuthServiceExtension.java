package org.dataportabilityproject.auth.microsoft;

import com.google.common.collect.ImmutableList;
import java.util.List;

import okhttp3.OkHttpClient;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.extension.AuthServiceExtension;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;

/** */
public class MicrosoftAuthServiceExtension implements AuthServiceExtension {
  private static final ImmutableList<String> DEFAULT_AUTH_GENERATOR_SERVICES =
      ImmutableList.of("contacts", "calendar");
  private volatile MicrosoftAuthDataGenerator authDataGenerator;

  public MicrosoftAuthServiceExtension() {
  }

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
      authDataGenerator =
          new MicrosoftAuthDataGenerator(
                  // TODO: Figure out the appropriate keys and objects
              context.getConfiguration("REDIRECT_PATH", (String) null),
              context.getConfiguration("MICROSOFT_KEY", (String) null),
              context.getConfiguration("MICROSOFT_SECRET", (String) null),
              /** OkhttpCLient */
              (OkHttpClient) null,
              context.getTypeManager().getMapper());
    }
    return authDataGenerator;
  }
}
