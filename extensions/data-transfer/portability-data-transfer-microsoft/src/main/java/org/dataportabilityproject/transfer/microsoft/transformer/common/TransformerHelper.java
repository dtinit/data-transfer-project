package org.dataportabilityproject.transfer.microsoft.transformer.common;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Utility methods for transforming data.
 */
public final class TransformerHelper {

    public static Optional<String> getString(String key, Map<String, ?> map) {
        return Optional.ofNullable((String) map.get(key));
    }

    @SuppressWarnings("unchecked")
    public static Optional<Map<String, String>> getMap(String key, Map<String, ?> map) {
        return Optional.ofNullable((Map<String, String>) map.get(key));
    }

    @SuppressWarnings("unchecked")
    public static Optional<List<String>> getList(String key, Map<String, ?> map) {
        return Optional.ofNullable((List<String>) map.get(key));
    }

    @SuppressWarnings("unchecked")
    public static Optional<List<Map<String, String>>> getListMap(String key, Map<String, ?> map) {
        return Optional.ofNullable((List<Map<String, String>>) map.get(key));
    }

    public static <K, V> void safeSet(K key, V value, Map<K, V> map) {
        if (value == null) {
            return;
        }
        map.put(key, value);
    }

    private TransformerHelper() {
    }
}
