package org.dataportabilityproject.webapp;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A class that contains all flags exlusive to the API server.
 */
public class PortabilityApiFlags {
  private static PortabilityApiFlags INSTANCE = null;

  private final String baseUrl; // TODO URL type that validates url
  private final String baseApiUrl; // TODO URL type that validates url

  private PortabilityApiFlags(
      String baseUrl,
      String baseApiUrl) {
    this.baseUrl = baseUrl;
    this.baseApiUrl = baseApiUrl;
  }

  /** Parse arguments and initialize the PortabilityFlags global configuration parameters. */
  public static void parse(String[] args) {
    // create Options object
    Options options = new Options();
    options.addOption("baseUrl", true, PortabilityApiFlags.baseUrlDesc());
    options.addOption("baseApiUrl", true, PortabilityApiFlags.baseApiUrlDesc());
    CommandLineParser parser = new DefaultParser();
    CommandLine cmd = null;
    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println("Fatal: Unable to parse commandline args: " + e);
      System.exit(1);
    }
    boolean encryptedFlow = false;
    String encryptedFlowStr = cmd.getOptionValue("encryptedFlow");
    String baseUrl = cmd.getOptionValue("baseUrl");
    String baseApiUrl = cmd.getOptionValue("baseApiUrl");

    boolean hasAllFlags = true;
    hasAllFlags &= !Strings.isNullOrEmpty(baseUrl);
    if (Strings.isNullOrEmpty(baseUrl)) {
      System.out.println("missing -baseUrl");
    } else {
      System.out.println("Parsed command line arg baseUrl = " + baseUrl);
    }
    hasAllFlags &= !Strings.isNullOrEmpty(baseApiUrl);
    if (Strings.isNullOrEmpty(baseApiUrl)) {
      System.out.println("missing -baseApiUrl");
    } else {
      System.out.println("Parsed command line arg baseApiUrl = " + baseApiUrl);
    }
    // Encrypted flow is optional
    if(!Strings.isNullOrEmpty(encryptedFlowStr)) {
      encryptedFlow = Boolean.parseBoolean(encryptedFlowStr);
    }
    if (!hasAllFlags) {
      help(options);
    }
    init(baseUrl, baseApiUrl, encryptedFlow);
  }

  private synchronized static void init(String baseUrl, String baseApiUrl, boolean encryptedFlow) {
    if (INSTANCE != null) {
      throw new IllegalStateException("Trying to initialize flags a second time");
    }

    INSTANCE = new PortabilityApiFlags(baseUrl, baseApiUrl);
  }

  private static void help(Options options) {
    HelpFormatter formater = new HelpFormatter();
    formater.printHelp("Main", options);
    System.exit(0);
  }

  public static String baseUrl() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'baseUrl' before flags have been initialized");
    return INSTANCE.baseUrl;
  }

  // TODO move to annotation on flag variable
  public static String baseUrlDesc() {
    return "Base url for all calls within the application";
  }

  public static String baseApiUrl() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'baseApiUrl' before flags have been initialized");
    return INSTANCE.baseApiUrl;
  }

  // TODO move to annotation on flag variable
  public static String baseApiUrlDesc() {
    return "Base url for direct to api calls within the application";
  }
}
