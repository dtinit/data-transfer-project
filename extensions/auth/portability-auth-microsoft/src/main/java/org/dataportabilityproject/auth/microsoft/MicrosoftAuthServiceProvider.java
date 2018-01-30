package org.dataportabilityproject.auth.microsoft;

import org.dataportabilityproject.spi.gateway.auth.AuthDataGenerator;
import org.dataportabilityproject.spi.gateway.auth.AuthServiceProvider;

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

    public AuthDataGenerator getAuthDataGenerator(String transferDataType) {
        return null;
    }
}
