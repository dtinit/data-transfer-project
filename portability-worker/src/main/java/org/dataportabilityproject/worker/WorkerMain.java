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

/**
 * Main class to bootstrap a portabilty worker that will operate on a single job whose state
 * is held in WorkerJobMetadata.
 */
public class WorkerMain {
  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(UncaughtExceptionHandlers.systemExit());
    Injector injector = Guice.createInjector(new WorkerModule());
    WorkerImpl worker = injector.getInstance(WorkerImpl.class);
    worker.processJob();
    System.exit(0);
  }
}
