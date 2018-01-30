/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.serviceProviders.rememberTheMilk.model;

import com.google.api.client.util.Key;

/**
 * Response from rtm.auth.getToken
 *
 * <p>Example: <?xml version='1.0' encoding='UTF-8'?><rsp stat="ok"><auth><token>0cefd358a1a6ec74b6a7a4f268681b27eb435916</token><perms>write</perms><user
 * id="1650840" username="bwillard" fullname="Brian Willard"/></auth></rsp>
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
