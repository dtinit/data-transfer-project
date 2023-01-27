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
package org.datatransferproject.types.client.transfer;

import java.util.Objects;
import org.datatransferproject.types.common.models.DataVertical;

/** Request to list services available for export and import for the given type. */
public class GetTransferServices {
  private final DataVertical transferDataType;

  public GetTransferServices(DataVertical transferDataType) {
    Objects.requireNonNull(transferDataType);
    this.transferDataType = transferDataType;
  }

  public DataVertical getTransferDataType() {
    return transferDataType;
  }
}
