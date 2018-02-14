package org.dataportabilityproject.auth.microsoft;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProvider;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProviderRegistry.AuthMode;

/**
 *
 */
public class MicrosoftAuthServiceProvider implements AuthServiceProvider {
    private MicrosoftAuthDataGenerator contactsGenerator;

    public MicrosoftAuthServiceProvider() {
        //contactsGenerator = new MicrosoftAuthDataGenerator(r)
    }

    public String getServiceId() {
        return "microsoft";
    }

    public AuthDataGenerator getAuthDataGenerator(String transferDataType, AuthMode mode) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getImportTypes() {
        return ImmutableList.of();
    }

    @Override
    public List<String> getExportTypes() {
        return ImmutableList.of();
    }
}
