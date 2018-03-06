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
package org.dataportabilityproject.shared;

import java.io.IOException;
import java.util.List;

/** Interface allowing services to display information, or ask questions to users */
public interface IOInterface {
  void print(String text);

  String ask(String prompt) throws IOException;

  /** Asks the user a multiple choice question. */
  <T> T ask(String prompt, List<T> choices) throws IOException;
}
