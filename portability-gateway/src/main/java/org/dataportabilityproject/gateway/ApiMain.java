/*
* Copyright 2018 The Data-Portability Project Authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* https://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.dataportabilityproject.gateway;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.lang.Thread.UncaughtExceptionHandler;
import org.dataportabilityproject.gateway.reference.ReferenceApiModule;
import org.dataportabilityproject.gateway.reference.ReferenceApiServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Starts the api server.
 */
public class ApiMain {
  private static final Logger logger = LoggerFactory.getLogger(ApiMain.class);

  /**
   * Starts the api server, currently the reference implementation.
   */
  public static void main(String[] args) throws Exception {
    logger.warn("Starting reference api server.");
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread thread, Throwable t) {
        logger.warn("Uncaught exception in thread: {}", thread.getName(), t);
      }
    });

    // TODO: Support other server implementations
    Injector injector = Guice.createInjector(new ReferenceApiModule());

    // Launch the application
    ReferenceApiServer server = injector.getInstance(ReferenceApiServer.class);
    server.start();
  }
}
