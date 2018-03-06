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
package org.dataportabilityproject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import org.dataportabilityproject.shared.IOInterface;

/** An {@link IOInterface} that interacts with the user via the console. */
public class ConsoleIO implements IOInterface {
  public void print(String text) {
    System.out.println(text);
  }

  public String ask(String prompt) throws IOException {
    System.out.println(prompt);
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    return reader.readLine();
  }

  /** Asks the user a multiple choice question. */
  public <T> T ask(String prompt, List<T> choices) throws IOException {
    System.out.println(prompt + " (enter the number of your choice):");
    for (int i = 0; i < choices.size(); i++) {
      System.out.println("\t" + i + ") " + choices.get(i));
    }
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    int userPick = Integer.parseInt(reader.readLine());
    return choices.get(userPick);
  }
}
