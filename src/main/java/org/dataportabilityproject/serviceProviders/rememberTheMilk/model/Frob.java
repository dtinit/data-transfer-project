package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

/**
 * Response from rtm.auth.getFrob
 *
 * <p>Example:
 * //<?xml version='1.0' encoding='UTF-8'?><rsp stat="ok"><frob>d27f3ecf5497d7fdd79aea0ba1ebe9bad375ce7b</frob></rsp>
 */
public class Frob extends RememberTheMilkResponse {
    @Key("frob")
    public String frob;

    @Override
    public String toString() {
        return "frob(stat=" + stat + ", frob=" + frob + ")";
    }
}
