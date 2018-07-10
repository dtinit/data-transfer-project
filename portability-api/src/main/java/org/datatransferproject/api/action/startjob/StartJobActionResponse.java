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
package org.datatransferproject.api.action.startjob;

import java.util.UUID;

/** The result of a data transfer submission action. */
public class StartJobActionResponse {

  private final UUID id;
  private final String errorMsg;

  private StartJobActionResponse(UUID id, String errorMsg) {
    this.id = id;
    this.errorMsg = errorMsg;
  }

  public static final StartJobActionResponse create(UUID id) {
    return new StartJobActionResponse(id, null);
  }

  public static final StartJobActionResponse createWithError(String errorMsg) {
    return new StartJobActionResponse(null, errorMsg);
  }

  public UUID getId() {
    return id;
  }

  public String getErrorMsg() {
    return errorMsg;
  }
}
