package org.dataportabilityproject.shared;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Utilities for Flag parsing
 */
public class FlagUtils {

  public static CommandLine parseFlags(String[] args, Options... options) {
    Options allOptions = new Options();
    for (Options ops : options) {
      for (Option o : ops.getOptions()) {
        allOptions.addOption(o);
      }
    }

    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;

    try {
      cmd = parser.parse(allOptions, args);
    } catch (ParseException e) {
      System.out.println("Fatal: Unable to parse commandline args: " + e);
      System.exit(1);
    }
    return cmd;
  }

}
