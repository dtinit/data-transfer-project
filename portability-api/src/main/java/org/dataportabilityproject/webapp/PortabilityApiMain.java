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
package org.dataportabilityproject.webapp;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class PortabilityApiMain {

  public static void main(String args[]) throws Exception {
    PortabilityApiFlags.parse();
    Injector injector = Guice.createInjector(new PortabilityApiModule());
    ApiServer apiServer = injector.getInstance(ApiServer.class);
    apiServer.start();
  }
}