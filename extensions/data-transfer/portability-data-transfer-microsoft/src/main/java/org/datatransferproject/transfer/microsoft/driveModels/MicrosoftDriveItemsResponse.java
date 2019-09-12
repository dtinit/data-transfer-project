/*
 * Copyright 2019 The Data Transfer Project Authors.
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

package org.datatransferproject.transfer.microsoft.driveModels;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned for Drive Item objects in Microsoft Graph API. Ref:
 * https://docs.microsoft.com/en-us/graph/api/resources/driveitem?view=graph-rest-1.0
 */
public class MicrosoftDriveItemsResponse {
  @JsonProperty("@odata.nextLink")
  private String nextPageLink;

  @JsonProperty("value")
  private MicrosoftDriveItem[] driveItems;

  public MicrosoftDriveItem[] getDriveItems() {
    return driveItems;
  }

  public String getNextPageLink() {
    return nextPageLink;
  }
}
