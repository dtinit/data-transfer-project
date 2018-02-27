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
package org.dataportabilityproject.spi.transfer.provider;

import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.DataModel;

/**
 * Imports data into a destination service.
 */
public interface Importer<A extends AuthData, T extends DataModel> {

    /**
     * Imports data.
     *
     * @param jobId the current job id
     * @param authData authentication information
     * @param data the data
     * @return the operation result
     */
    ImportResult importItem(String jobId, A authData, T data); // REVIEW: The original throws IOException. Continue to use or return as part of the result?

}
