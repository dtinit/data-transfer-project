package org.dataportabilityproject.transfer.microsoft.transformer;

import ezvcard.VCard;
import ezvcard.property.Address;
import org.dataportabilityproject.transfer.microsoft.transformer.contacts.ToGraphAddressTransformer;
import org.dataportabilityproject.transfer.microsoft.transformer.contacts.ToGraphContactTransformer;
import org.dataportabilityproject.transfer.microsoft.transformer.contacts.ToVCardAddressTransformer;
import org.dataportabilityproject.transfer.microsoft.transformer.contacts.ToVCardTransformer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 *
 */
public class TransformerServiceImpl implements TransformerService {
    Map<TransformKey, BiFunction<?, ?, ?>> cache = new HashMap<>();

    public TransformerServiceImpl() {
        initContactTransformers();
    }

    @Override
    public <T> TransformResult<T> transform(Class<T> resultType, Object input) {
        TransformerContext context = new TransformerContextImpl();
        T dataType = transform(resultType, input, context);
        return new TransformResult<>(dataType, context.getProblems());
    }

    @SuppressWarnings("unchecked")
    private <T> T transform(Class<T> resultType, Object input, TransformerContext context) {
        Objects.requireNonNull(resultType, "No result type specified");
        Objects.requireNonNull(input, "No input specified");
        TransformKey key = new TransformKey(input.getClass(), resultType);
        BiFunction<Object, TransformerContext, T> function = (BiFunction<Object, TransformerContext, T>) cache.computeIfAbsent(key, v -> {
            throw new IllegalArgumentException("Unsupported transformation: " + input.getClass().getName() + ":" + resultType);
        });
        return function.apply(input, context);
    }

    private class TransformerContextImpl implements TransformerContext {
        private List<String> problems = new ArrayList<>();

        @Override
        public <T> T transform(Class<T> resultType, Object input, TransformerContext context) {
            if (input == null) {
                return null;  // support null
            }
            return TransformerServiceImpl.this.transform(resultType, input, context);
        }

        @Override
        public void problem(String message) {
            problems.add(message);
        }

        @Override
        public List<String> getProblems() {
            return problems;
        }
    }

    private void initContactTransformers() {
        cache.put(new TransformKey(LinkedHashMap.class, VCard.class), new ToVCardTransformer());
        cache.put(new TransformKey(LinkedHashMap.class, Address.class), new ToVCardAddressTransformer());
        cache.put(new TransformKey(VCard.class, LinkedHashMap.class), new ToGraphContactTransformer());
        cache.put(new TransformKey(Address.class, LinkedHashMap.class), new ToGraphAddressTransformer());
    }

    private class TransformKey {
        private Class<?> from;
        private Class<?> to;

        public TransformKey(Class<?> from, Class<?> to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransformKey that = (TransformKey) o;
            return Objects.equals(from, that.from) &&
                    Objects.equals(to, that.to);
        }

        @Override
        public int hashCode() {
            return Objects.hash(from, to);
        }
    }

}
