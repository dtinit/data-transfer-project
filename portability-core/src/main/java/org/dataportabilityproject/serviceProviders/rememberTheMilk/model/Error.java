package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

/**
 * Error details from RTM
 */
public class Error {
    @Key("@code")
    public int code;

    @Key("@msg")
    public String msg;

    @Override
    public String toString() {
        return String.format("Error(code=%d msg=%s)", code, msg);
    }
}
