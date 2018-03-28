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

package org.dataportabilityproject.transfer.rememberthemilk;

import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;

public class RememberTheMilkTransferExtension implements TransferExtension {
  @Override
  public String getServiceId() {
    return null;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    return null;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    return null;
  }

  @Override
  public void initialize(ExtensionContext context) {

  }
}
