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
package org.datatransferproject.transfer.copier;

import org.datatransferproject.spi.transfer.types.CopyException;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.transfer.auth.AuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory Copier interface
 */
public interface InMemoryDataCopier {

    /* Copies the provided dataType from exportService to importService */
    void copy(AuthData exportAuthData, AuthData importAuthData, UUID jobId, Optional<ExportInformation> exportInfo) throws IOException, CopyException;

    Collection<ErrorDetail> getErrors(UUID jobId);
}
