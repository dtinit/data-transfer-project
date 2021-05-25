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
package org.datatransferproject.spi.transfer.hooks;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/** Helper for loading job hooks extensions. */
public class JobHooksLoader {

  public static JobHooks loadJobHooks() {
    List<JobHooks> jobHooks = new ArrayList<>();
    ServiceLoader.load(JobHooksExtension.class)
        .iterator()
        .forEachRemaining(
            extension -> {
              extension.initialize();
              jobHooks.add(extension.getJobHooks());
            });
    return jobHooks.isEmpty()
        ? new DefaultJobHooks()
        : new MultiplexJobHooks(jobHooks.toArray(new JobHooks[0]));
  }

  private JobHooksLoader() {}
}
