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
package org.dataportabilityproject.transfer.microsoft.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import okhttp3.OkHttpClient;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProvider;
import org.dataportabilityproject.transfer.microsoft.contacts.MicrosoftContactsExporter;
import org.dataportabilityproject.transfer.microsoft.contacts.MicrosoftContactsImporter;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerServiceImpl;

/**
 *
 */
public class MicrosoftTransferServiceProvider implements TransferServiceProvider {
    // TODO we should standardize the naming scheme if it isn't already
    private static final String CONTACTS = "contacts";

    private Map<String, Exporter<?, ?>> exporterCache = new HashMap<>();
    private Map<String, Importer<?, ?>> importerCache = new HashMap<>();

    public MicrosoftTransferServiceProvider() {
        // TODO: inject these params
        OkHttpClient client = new OkHttpClient.Builder().build();
        ObjectMapper mapper = new ObjectMapper();
        TransformerService transformerService = new TransformerServiceImpl();
        exporterCache.put(CONTACTS,
            new MicrosoftContactsExporter(client, mapper, transformerService));
        importerCache.put(CONTACTS,
            new MicrosoftContactsImporter("", client, mapper, transformerService));
    }

    @Override
    public String getServiceId() {
        return "microsoft";
    }

    @Override
    public Exporter<?, ?> getExporter(String transferDataType) {
        return exporterCache.computeIfAbsent(transferDataType, v -> {
            throw new IllegalArgumentException("Unsupported export type: " + transferDataType);
        });
    }

    @Override
    public Importer<?, ?> getImporter(String transferDataType) {
        return importerCache.computeIfAbsent(transferDataType, v -> {
            throw new IllegalArgumentException("Unsupported import type: " + transferDataType);
        });
    }

    @Override
    public List<String> getImportTypes(){
        return ImmutableList.of(CONTACTS);
    }

    @Override
    public List<String> getExportTypes(){
        return ImmutableList.of(CONTACTS);
    }
}
