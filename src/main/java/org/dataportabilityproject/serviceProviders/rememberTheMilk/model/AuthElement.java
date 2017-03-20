package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

/**
 * Response from rtm.auth.getToken
 *
 * <p>Example:
 * <?xml version='1.0' encoding='UTF-8'?><rsp stat="ok"><auth><token>0cefd358a1a6ec74b6a7a4f268681b27eb435916</token><perms>write</perms><user id="1650840" username="bwillard" fullname="Brian Willard"/></auth></rsp>
 */
public class AuthElement extends RememberTheMilkResponse {
    @Key("auth")
    public Auth auth;

    @Override
    public String toString() {
        return "Auth(stat=" + stat + ", auth=" + auth + ")";
    }

    public static class Auth {
        @Key("token")
        public String token;

        @Key("perms")
        public String perms;

        @Key("user")
        public User user;

        @Override
        public String toString() {
            return "Auth(token=" + token + ", perms=" + perms + ", user=" + user + ")";
        }
    }

    public static class User {
        @Key("@id")
        public int id;

        @Key("@username")
        public String username;

        @Key("@fullname")
        public String fullname;

        @Override
        public String toString() {
            return "User(id=" + id + ", username=" + username + ", fullname=" + fullname + ")";
        }
    }
}
