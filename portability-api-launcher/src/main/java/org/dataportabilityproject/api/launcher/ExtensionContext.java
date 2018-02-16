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
 * Provides information required to bootstrap extensions.
 */
public interface ExtensionContext {

    /**
     * Returns the system logger extension service can use to record events.
     */
    Logger getLogger();

    /**
     * Returns a system service such as a type mapper extension services may require.
     *
     * @param type the system service type
     */
    <T> T getService(Class<T> type);

    /**
     * Returns the configuration parameter for the key or the default value if not found.
     *
     * @param key the parameter key
     * @param defaultValue the default value. Null may be passed.
     */
    <T> T getConfiguration(String key, String defaultValue);

}
