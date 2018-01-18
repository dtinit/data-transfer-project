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
package org.dataportabilityproject.serviceProviders.rememberTheMilk;


enum RememberTheMilkMethods {
  CHECK_TOKEN("rtm.auth.checkToken"),
  GET_FROB("rtm.auth.getFrob"),
  LISTS_GET_LIST("rtm.lists.getList"),
  LISTS_ADD("rtm.lists.add"),
  GET_TOKEN("rtm.auth.getToken"),
  TASKS_GET_LIST("rtm.tasks.getList"),
  TASK_ADD("rtm.tasks.add"),
  TIMELINES_CREATE("rtm.timelines.create"),;

  private static final String BASE_URL = "https://api.rememberthemilk.com/services/rest/";
  private final String methodName;

  RememberTheMilkMethods(String methodName) {
    this.methodName = methodName;
  }

  String getMethodName() {
    return methodName;
  }

  String getUrl() {
    return BASE_URL + "?method=" + getMethodName();
  }

}
