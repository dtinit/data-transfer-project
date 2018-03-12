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
package org.dataportabilityproject.worker;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.UncaughtExceptionHandlers;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.cloud.google.GoogleCloudExtension;
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.service.extension.ServiceExtension;

import java.util.ServiceLoader;

/**
 * Main class to bootstrap a portability worker that will operate on a single job whose state is
 * held in {@link JobMetadata}.
 */
public class WorkerMain {
  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

    ExtensionContext context = new WorkerExtensionContext();

    ServiceLoader.load(ServiceExtension.class)
        .iterator()
        .forEachRemaining(
            serviceExtension -> serviceExtension.initialize(context));

    CloudExtension cloudExtension = getCloudExtension();
    cloudExtension.initialize(context);

    Injector injector =
        Guice.createInjector(new WorkerModule(cloudExtension, context));
    Worker worker = injector.getInstance(Worker.class);
    worker.doWork();

    System.exit(0);
  }

  private static CloudExtension getCloudExtension() {
    ImmutableList.Builder<CloudExtension> extensionsBuilder = ImmutableList.builder();
    // TODO(seehamrun): Service load cloud extension and remove hardcoded GoogleCloudExtension
    // ServiceLoader.load(CloudExtension.class).iterator().forEachRemaining(extensionsBuilder::add);
    extensionsBuilder.add(new GoogleCloudExtension());
    ImmutableList<CloudExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        extensions.size() == 1,
        "Exactly one CloudExtension is required, but found " + extensions.size());
    return extensions.get(0);
  }
}
