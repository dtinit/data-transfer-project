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

import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dataportabilityproject.PortabilityFlags;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.shared.Config.Environment;
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
  public static void main(String[] args) throws ParseException, InterruptedException {
    parseArgs(args);
    SpringApplication.run(PortabilityServer.class, args);
  }

  private static void parseArgs(String[] args) {
    // create Options object
    Options options = new Options();
    // TODO set & get "cloud" and "environment" from annotation on flag in PortabilityFlags
    options.addOption("cloud", true, PortabilityFlags.cloudFlagDesc());
    options.addOption("environment", true, PortabilityFlags.environmentFlagDesc());
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println("Unable to parse commandline args: " + e);
      System.exit(1);
    }
    String cloud = cmd.getOptionValue("cloud");
    String environment = cmd.getOptionValue("environment");

    boolean hasAllFlags = true;
    hasAllFlags &= !Strings.isNullOrEmpty(cloud);
    if (Strings.isNullOrEmpty(cloud)) {
      System.out.println("missing -cloud");
    } else {
      System.out.println("Parsed command line arg cloud = " + cloud);
    }
    hasAllFlags &= !Strings.isNullOrEmpty(environment);
    if (Strings.isNullOrEmpty(environment)) {
      System.out.println("missing -environment");
    } else {
      System.out.println("Parsed command line arg environment = " + environment);
    }
    if (!hasAllFlags) {
      help(options);
    }
    PortabilityFlags.init(SupportedCloud.valueOf(cloud), Environment.valueOf(environment));
  }

  private static void help(Options options) {
    HelpFormatter formater = new HelpFormatter();
    formater.printHelp("Main", options);
    System.exit(0);
  }
}
