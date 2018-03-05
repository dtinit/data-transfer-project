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
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import java.util.List;
import java.util.ServiceLoader;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProvider;
import org.dataportabilityproject.transfer.microsoft.provider.MicrosoftTransferServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class to bootstrap a portability worker that will operate on a single job whose state
 * is held in WorkerJobMetadata.
 */
public class WorkerMain {
    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

        ExtensionContext extensionContext = getExtensionContext();
        // TODO: logger = extensionContext.getLogger();
        Logger logger = LoggerFactory.getLogger(WorkerMain.class);

        Injector injector = Guice.createInjector(new WorkerModule());
        WorkerImpl worker = injector.getInstance(WorkerImpl.class);

        // Poll job, and determine its data type, import service, and export service
        worker.pollForJob();
        WorkerJobMetadata metadata = worker.getWorkerJobMetadata();
        String dataType = metadata.getDataType();
        String exportService = metadata.getExportService();
        String importService = metadata.getImportService();
        Exporter<?, ?> exporter = null;
        Importer<?, ?> importer = null;

        for (TransferServiceProvider transferServiceProvider : listAllTransferServiceProviders()) {
            if (transferServiceProvider.getServiceId().equals(exportService)) {
                logger.info("Found transfer service provider for export service {}: {}",
                exportService, transferServiceProvider.getClass().getName());
                Preconditions.checkState(exporter == null, "We should only have one valid exporter "
                    + "for this job, but already have another one: "
                    + exporter.getClass().getName() + " and the exporter for "
                    + transferServiceProvider.getServiceId() + " also matches");

                exporter = transferServiceProvider.getExporter(dataType);

                Preconditions.checkNotNull(importer, String.format("Export TransferServiceProvider "
                        + "%s does not contain an exporter for job %s's dataType, %s!",
                    transferServiceProvider.getClass().toString(), metadata.getJobId(),
                    dataType));
                logger.info("Found exporter for dataType {}: {}", dataType,
                    exporter.getClass().getName());
            } else if (transferServiceProvider.getServiceId().equals(importService)) {
                logger.info("Found transfer service provider for import service {}: {}",
                    importService, transferServiceProvider.getClass().getName());
                Preconditions.checkState(exporter == null, "We should only have one valid importer "
                    + "for this job. We already have one (" + importer.getClass().getName()
                    + ") but the service provider for " + transferServiceProvider.getServiceId()
                    + " also matches");

                importer = transferServiceProvider.getImporter(dataType);

                Preconditions.checkNotNull(importer, String.format("Import TransferServiceProvider "
                        + "%s does not contain an importer for job %s's dataType, %s!",
                        transferServiceProvider.getClass().toString(), metadata.getJobId(),
                        dataType));
                logger.info("Found importer for dataType {}: {}", dataType,
                    importer.getClass().getName());
            }
        }
        Preconditions.checkNotNull(exporter, String.format("No %s exporter found for %s",
            dataType, exportService));
        Preconditions.checkNotNull(importer, String.format("No %s importer found for %s",
            dataType, importService));
        // Dynamically create a TransferExtension with the correct importer and exporter, based on
        // our polled job
        // TODO: load this as a single class rather than list, need to figure out the syntax
        List<TransferExtension> extensions =
            ImmutableList.of(createTransferExtension(exporter, importer));

        ServiceLoader.load(TransferExtension.class).iterator().forEachRemaining(extensions::add);
        for (TransferExtension extension : extensions) {
            extension.initialize(extensionContext);
        }
        for (TransferExtension extension : extensions) {
            extension.start();
        }

        worker.processJob();

        for (TransferExtension extension : extensions) {
            extension.shutdown();
        }

        System.exit(0);
    }

    // TODO: consider getting this dynamically rather than hardcoding
    private static List<TransferServiceProvider> listAllTransferServiceProviders() {
        // TODO if we get these dynamically, consider validating all service names unique
        return ImmutableList.of(new MicrosoftTransferServiceProvider());
    }

    private static TransferExtension createTransferExtension(Exporter exporter, Importer importer) {
        return new TransferExtension() {
            private boolean initialized = false;

            @Override
            public Exporter<?, ?> getExporter() {
                Preconditions.checkState(initialized,
                    "TransferExtension must be initialized before using its exporter");
                return exporter;
            }

            @Override
            public Importer<?, ?> getImporter() {
                Preconditions.checkState(initialized,
                    "TransferExtension must be initialized before using its importer");
                return importer;
            }

            @Override
            public void initialize(ExtensionContext context) {
                // TODO: implement
                initialized = true;
            }
        };
    }

    private static ExtensionContext getExtensionContext() {
        return new ExtensionContext() {
            @Override
            public org.dataportabilityproject.api.launcher.Logger getLogger() {
                return new org.dataportabilityproject.api.launcher.Logger() {};
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
