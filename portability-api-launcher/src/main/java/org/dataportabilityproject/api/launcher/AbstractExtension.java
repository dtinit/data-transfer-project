/*
 * Copyright 2018 The Data-Portability Project Authors.
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
package org.dataportabilityproject.api.launcher;

/**
 * Implementations provide an extension to the system.
 */
public interface AbstractExtension {
    /**
     * Initializes the extension. Implementations prepare provided services.
     *
     * @param context the extension context.
     */
    void initialize(ExtensionContext context);

    /**
     * Signals to the extension to prepare for receiving requests. For example, implementations may
     * open sockets or other resources.
     */
    default void start() {
    }

    /**
     * Signals the extension to terminate and cleanup resources.
     */
    default void shutdown() {
    }

}
