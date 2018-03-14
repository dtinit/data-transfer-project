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
import org.dataportabilityproject.spi.cloud.extension.CloudExtension;
import org.dataportabilityproject.spi.service.extension.ServiceExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ServiceLoader;

/**
 * Main class to bootstrap a portability worker that will operate on a single job whose state is
 * held in {@link JobMetadata}.
 */
public class WorkerMain {
  private static final Logger logger = LoggerFactory.getLogger(WorkerMain.class);

  private Worker worker;

  public static void main(String[] args) {
    Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());

    WorkerMain workerMain = new WorkerMain();
    workerMain.initialize();
    workerMain.poll();

    System.exit(0);
  }

  public void initialize() {
    ExtensionContext context = new WorkerExtensionContext();

    ServiceLoader.load(ServiceExtension.class)
        .iterator()
        .forEachRemaining(serviceExtension -> serviceExtension.initialize(context));

    CloudExtension cloudExtension = getCloudExtension();
    // TODO: verify that this is the cloud extension that is specified in the configuration
    logger.info("Using CloudExtension: {} ", cloudExtension.getClass().getName());

    cloudExtension.initialize(context);

    Injector injector = Guice.createInjector(new WorkerModule(cloudExtension, context));
    worker = injector.getInstance(Worker.class);
  }

  public void poll() {
    worker.doWork();
  }

  private static CloudExtension getCloudExtension() {
    ImmutableList.Builder<CloudExtension> extensionsBuilder = ImmutableList.builder();
    ServiceLoader.load(CloudExtension.class).iterator().forEachRemaining(extensionsBuilder::add);
    ImmutableList<CloudExtension> extensions = extensionsBuilder.build();
    Preconditions.checkState(
        extensions.size() == 1,
        "Exactly one CloudExtension is required, but found " + extensions.size());
    return extensions.get(0);
  }
}
