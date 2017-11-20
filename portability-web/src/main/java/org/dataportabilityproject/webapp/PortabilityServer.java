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

import java.io.IOException;
import org.apache.commons.cli.ParseException;
import org.dataportabilityproject.PortabilityFlags;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * To run:
 * java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 \
 * -jar portability-web/target/portability-web-1.0-SNAPSHOT.jar -cloud GOOGLE -environment LOCAL
 */
@SpringBootApplication
public class PortabilityServer {

  /** Starts the Portability web server. */
  public static void main(String[] args) throws ParseException, InterruptedException, IOException {
    PortabilityFlags.parse(args);
    SpringApplication.run(PortabilityServer.class, args);
  }
}
