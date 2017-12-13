package org.dataportabilityproject;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dataportabilityproject.cloud.SupportedCloud;
import org.dataportabilityproject.shared.Config.Environment;

/**
 * A class that contains all flags passed from the commandline.
 */
public class PortabilityFlags {

  private final static Options OPTIONS = new Options()
      .addOption("cloud", true, PortabilityFlags.cloudFlagDesc())
      .addOption("environment", true, PortabilityFlags.environmentFlagDesc());
  private static PortabilityFlags INSTANCE = null;
  private final SupportedCloud cloud;
  private final Environment environment;
  private final boolean encryptedFlow;  // default is off until flow is completed

  private PortabilityFlags(
      SupportedCloud cloud,
      Environment environment,
      boolean encryptedFlow) {
    this.cloud = cloud;
    this.environment = environment;
    this.encryptedFlow = encryptedFlow;
  }

  /* Returns the group of options necessary for PortabilityFlags*/
  public static Options getOptions() {
    return OPTIONS;
  }

  /**
   * Initialize the PortabilityFlags global configuration parameters from provided command line.
   */
  public static void parse(CommandLine cmd) {
    String cloud = cmd.getOptionValue("cloud");
    String environment = cmd.getOptionValue("environment");
    String encryptedFlowStr = cmd.getOptionValue("encryptedFlow");

    if (!checkRequiredArgs(cloud, environment)) {
      help(getOptions());
    }

    // Encrypted flow is optional
    boolean encryptedFlow = false;
    if (!Strings.isNullOrEmpty(encryptedFlowStr)) {
      encryptedFlow = Boolean.parseBoolean(encryptedFlowStr);
    }

    init(SupportedCloud.valueOf(cloud), Environment.valueOf(environment), encryptedFlow);
  }

  private synchronized static void init(SupportedCloud supportedCloud, Environment environment,
      boolean encryptedFlow) {
    if (INSTANCE != null) {
      throw new IllegalStateException("Trying to initialize flags a second time");
    }

    INSTANCE = new PortabilityFlags(supportedCloud, environment, encryptedFlow);
  }

  private static boolean checkRequiredArgs(String cloud, String environment) {
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

    return hasAllFlags;
  }

  private static void help(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("Main", options);
    System.exit(0);
  }

  public static SupportedCloud cloud() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'cloud' before flags have been initialized");
    return INSTANCE.cloud;
  }

  // TODO move to annotation on flag variable
  private static String cloudFlagDesc() {
    return "Which storage to use. Can be 'LOCAL' for in memory or 'Google' for Google Cloud";
  }

  public static Environment environment() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'environment' before flags have been initialized");
    return INSTANCE.environment;
  }

  public static boolean encryptedFlow() {
    Preconditions.checkNotNull(INSTANCE,
        "Trying to get 'encryptedFlow' before flags have been initialized");
    return INSTANCE.encryptedFlow;
  }

  // TODO move to annotation on flag variable
  private static String environmentFlagDesc() {
    return "The deployment environment. One of LOCAL, TEST, QA, or PROD.";
  }
}
