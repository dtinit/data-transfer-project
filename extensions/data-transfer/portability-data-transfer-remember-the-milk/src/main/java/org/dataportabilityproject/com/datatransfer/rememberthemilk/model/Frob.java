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
 * Response from rtm.auth.getFrob
 *
 * <p>Example: //<?xml version='1.0' encoding='UTF-8'?><rsp stat="ok"><frob>d27f3ecf5497d7fdd79aea0ba1ebe9bad375ce7b</frob></rsp>
 */
public class Frob extends RememberTheMilkResponse {

  @Key("frob")
  public String frob;

  @Override
  public String toString() {
    return "frob(stat=" + stat + ", frob=" + frob + ")";
  }
}
