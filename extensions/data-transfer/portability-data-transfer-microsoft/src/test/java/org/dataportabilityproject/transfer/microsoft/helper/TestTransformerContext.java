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
package org.dataportabilityproject.transfer.microsoft.helper;

import org.dataportabilityproject.transfer.microsoft.transformer.TransformerContext;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerServiceImpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class TestTransformerContext implements TransformerContext {
    private Map<String, String> properties = new HashMap<>();
    private TransformerService transformerService = new TransformerServiceImpl();

    @Override
    public <T> T transform(Class<T> resultType, Object input) {
        return transformerService.transform(resultType, input, properties).getTransformed();
    }

    @Override
    public String getProperty(String key) {
        return properties.get(key);
    }

    @Override
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    @Override
    public void problem(String message) {

    }

    @Override
    public List<String> getProblems() {
        return null;
    }
}
