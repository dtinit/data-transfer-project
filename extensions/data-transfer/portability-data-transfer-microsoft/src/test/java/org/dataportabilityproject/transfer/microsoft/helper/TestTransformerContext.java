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
