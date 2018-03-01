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

import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Logger;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Main class to bootstrap a portabilty worker that will operate on a single job whose state
 * is held in WorkerJobMetadata.
 */
public class WorkerMain {
    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

        ExtensionContext extensionContext = getExtensionContext();

        List<TransferExtension> extensions = new ArrayList<>();
        ServiceLoader.load(TransferExtension.class).iterator().forEachRemaining(extensions::add);

        for (TransferExtension extension : extensions) {
            extension.initialize(extensionContext);
        }
        for (TransferExtension extension : extensions) {
            extension.getExporters();
            extension.getImporters();
        }
        for (TransferExtension extension : extensions) {
            extension.start();
        }

        Injector injector = Guice.createInjector(new WorkerModule());
        WorkerImpl worker = injector.getInstance(WorkerImpl.class);
        worker.processJob();

        for (TransferExtension extension : extensions) {
            extension.shutdown();
        }

        System.exit(0);
    }

    private static ExtensionContext getExtensionContext() {
        return new ExtensionContext() {
            @Override
            public Logger getLogger() {
                return new Logger() {{
                }};
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
