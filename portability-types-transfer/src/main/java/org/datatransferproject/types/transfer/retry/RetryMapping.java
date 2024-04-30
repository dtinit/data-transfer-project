/*
 * Copyright 2018 The Data Transfer Project Authors.
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

package org.datatransferproject.types.transfer.retry;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Throwables.getStackTraceAsString;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that determines whether a given {@link Throwable} is a match for its {@link RetryStrategy}.
 * At the moment, this class only examines the string of the Throwable and determines whether it
 * matches any of its string regular expressions.
 *
 * <p>NOTE: Our core library only supports reading RetryMappings from JSON or YAML format.
 */
public class RetryMapping {

  @JsonProperty("regexes")
  private String[] regexes;

  @JsonProperty("stacktraceRegexes")
  private String[] stacktraceRegexes;

  @JsonProperty(value = "strategy", required = true)
  private RetryStrategy strategy;

  public RetryMapping(
      @JsonProperty("regexes") String[] regexes,
      @JsonProperty("stacktraceRegexes") String[] stacktraceRegexes,
      @JsonProperty("strategy") RetryStrategy strategy) {
    this.regexes = regexes;
    this.stacktraceRegexes = stacktraceRegexes;
    this.strategy = strategy;
    checkArgument(
        this.regexes != null || this.stacktraceRegexes != null,
        "either regexes or stacktraceRegexes must be set for a valid RetryMapping");
  }

  public String[] getRegexes() {
    return regexes;
  }

  public String[] getStacktraceRegexes() {
    return stacktraceRegexes;
  }

  public RetryStrategy getStrategy() {
    return strategy;
  }

  /**
   * Deprecated method to determine if throwable is matched by the config this class was constructed
   * with.
   *
   * <ul>
   *   Deprecated in favor of {@link matchesThrowableTop} for old behavior, or more precisely:
   *   please use...
   *   <li>{@link matches} to handle either older style matching or new style matching, whichever
   *       the YAML dictates. This is the most likely correct choice.
   *   <li>{@link matchesThrowableTop} to handle either older style matching only.
   *   <li>{@link matchesThrowableStack} to handle only newer style matching (full stack trace).
   * </ul>
   */
  public boolean matchesThrowable(Throwable throwable) {
    return matchesThrowableTop(throwable);
  }

  /** Whether throwable is matched by any of the current or future capabilities of this class. */
  public boolean matches(Throwable throwable) {
    // nit: we're purposely trying to short-circuit here on an early match with a much smaller
    // haystack (the top-level message).
    return matchesThrowableTop(throwable) || matchesThrowableStack(throwable);
  }

  /**
   * Whether throwable matches the top-level (ie: most recent, or high-stack-level propogator) of
   * this stack trace.
   */
  public boolean matchesThrowableTop(Throwable throwable) {
    if (regexes == null) {
      return false;
    }

    return throwableMatchesExpressions(throwable.toString(), regexes);
  }

  /**
   * More lenient version of {@link matchesThrowableTop} that checks not just for matches against
   * the top-level message in the stack that Throwable comprises, but for a match _anywhere_ in the
   * full stack trace.
   */
  public boolean matchesThrowableStack(Throwable throwable) {
    if (stacktraceRegexes == null) {
      return false;
    }

    return throwableMatchesExpressions(getStackTraceAsString(throwable), stacktraceRegexes);
  }

  private static boolean throwableMatchesExpressions(
      String throwableHaystack, String[] expressions) {
    for (String regex : expressions) {
      if (isMultilineMatch(regex, throwableHaystack)) {
        return true;
      }
    }
    return false;
  }

  /** Whether hayStack contains the regex, even if '.' operator needs to match linebreaks. */
  private static boolean isMultilineMatch(String regex, String hayStack) {
    // Identical to {@link String#matches} but utilizes {@link Pattern.DOTALL} for regex
    // compilation.
    Pattern p = Pattern.compile(regex, Pattern.DOTALL);
    Matcher m = p.matcher(hayStack);
    return m.matches();
  }

  @Override
  public String toString() {
    return "RetryMapping{"
        + "regexes="
        + Arrays.toString(regexes)
        + ", stacktraceRegexes="
        + Arrays.toString(stacktraceRegexes)
        + ", strategy="
        + strategy
        + '}';
  }
}
