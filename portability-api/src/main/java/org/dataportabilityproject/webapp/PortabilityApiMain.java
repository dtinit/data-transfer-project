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

import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.lang.Thread.UncaughtExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortabilityApiMain {
  private static final Logger logger = LoggerFactory.getLogger(PortabilityApiMain.class);
  public static void main(String args[]) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){
      @Override
      public void uncaughtException(Thread thread, Throwable t) {
        logger.warn("Uncaught exception in thread: {}", thread.getName(), t);
      }
    });
    PortabilityApiFlags.parse();
    Injector injector = Guice.createInjector(new PortabilityApiModule());
    ApiServer apiServer = injector.getInstance(ApiServer.class);
    apiServer.start();
  }
}