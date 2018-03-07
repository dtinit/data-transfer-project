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
package org.dataportabilityproject.worker;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;

/**
 * Main class to bootstrap a portability worker that will operate on a single job whose state is
 * held in WorkerJobMetadata.
 */
public class WorkerMain {
  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

    ExtensionContext extensionContext = getExtensionContext();

    Injector injector = Guice.createInjector(new WorkerModule());
    WorkerImpl worker = injector.getInstance(WorkerImpl.class);

    // Poll job, and determine its data type, import service, and export service
    worker.pollForJob();
    WorkerJobMetadata metadata = worker.getWorkerJobMetadata();
    String transferDataType = metadata.getDataType();
    String exportService = metadata.getExportService();
    String importService = metadata.getImportService();
    Exporter<?, ?> exporter = null;
    Importer<?, ?> importer = null;

    // TODO: Next version should ideally not load every TransferExtension impl, look into
    // solutions where we selectively invoke class loader.
    List<TransferExtension> extensions = new ArrayList<>();
    ServiceLoader.load(TransferExtension.class).iterator().forEachRemaining(extensions::add);
    Preconditions.checkState(
        !extensions.isEmpty(), "Could not find any implementations of TransferExtension");

    // TODO: This could be simplified with streams & filtering
    for (TransferExtension extension : extensions) {
      if (extension.getServiceId().equals(exportService)) {
        extension.initialize(extensionContext);
        Preconditions.checkState(
            exporter == null,
            "Can only instantiate exporter from one matching TransferExtension. Previously found :"
            + exporter.getClass() + ", and now found: " + extension.getClass());
        exporter = extension.getExporter(transferDataType);
      } else if (extension.getServiceId().equals(importService)) {
        extension.initialize(extensionContext);
        Preconditions.checkState(
            importer == null,
            "Can only instantiate importer from one matching TransferExtension. Previously found :"
                + importer.getClass() + ", and now found: " + extension.getClass());
        importer = extension.getImporter(transferDataType);
      }
    }

    Preconditions.checkNotNull(
        exporter, String.format("No %s exporter found for %s", transferDataType, exportService));
    Preconditions.checkNotNull(
        importer, String.format("No %s importer found for %s", transferDataType, importService));

    worker.processJob(exporter, importer);

    System.exit(0);
  }

  private static ExtensionContext getExtensionContext() {
    return new ExtensionContext() {
      @Override
      public Logger getLogger() {
        return new Logger() {};
      }

      @Override
      public <T> T getService(Class<T> type) {
        return null;
      }

      @Override
      public <T> T getConfiguration(String key, String defaultValue) {
        return null;
      }
    };
  }
}
