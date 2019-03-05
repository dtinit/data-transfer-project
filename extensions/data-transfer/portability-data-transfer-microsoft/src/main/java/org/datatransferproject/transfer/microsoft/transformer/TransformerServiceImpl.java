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
package org.datatransferproject.transfer.microsoft.transformer;

import ezvcard.VCard;
import ezvcard.property.Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import org.datatransferproject.transfer.microsoft.transformer.calendar.ToCalendarAttendeeModelTransformer;
import org.datatransferproject.transfer.microsoft.transformer.calendar.ToCalendarEventModelTransformer;
import org.datatransferproject.transfer.microsoft.transformer.calendar.ToCalendarEventTimeTransformer;
import org.datatransferproject.transfer.microsoft.transformer.calendar.ToCalendarModelTransformer;
import org.datatransferproject.transfer.microsoft.transformer.calendar.ToGraphCalendarTransformer;
import org.datatransferproject.transfer.microsoft.transformer.calendar.ToGraphEventTransformer;
import org.datatransferproject.transfer.microsoft.transformer.contacts.ToGraphAddressTransformer;
import org.datatransferproject.transfer.microsoft.transformer.contacts.ToGraphContactTransformer;
import org.datatransferproject.transfer.microsoft.transformer.contacts.ToVCardAddressTransformer;
import org.datatransferproject.transfer.microsoft.transformer.contacts.ToVCardTransformer;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;

/** Dispatches to a cached function based on input and result types to perform a transformation. */
public class TransformerServiceImpl implements TransformerService {
  Map<TransformKey, BiFunction<?, ?, ?>> cache = new HashMap<>();

  public TransformerServiceImpl() {
    initContactTransformers();
    initCalendarTransformers();
  }

  @Override
  public <T> TransformResult<T> transform(Class<T> resultType, Object input) {
    return transform(resultType, input, new HashMap<>());
  }

  @Override
  public <T> TransformResult<T> transform(
      Class<T> resultType, Object input, Map<String, String> properties) {
    TransformerContext context = new TransformerContextImpl(properties);
    T dataType = transform(resultType, input, context);
    return new TransformResult<>(dataType, context.getProblems());
  }

  @SuppressWarnings("unchecked")
  private <T> T transform(Class<T> resultType, Object input, TransformerContext context) {
    Objects.requireNonNull(resultType, "No result type specified");
    Objects.requireNonNull(input, "No input specified");
    TransformKey key = new TransformKey(input.getClass(), resultType);
    BiFunction<Object, TransformerContext, T> function =
        (BiFunction<Object, TransformerContext, T>)
            cache.computeIfAbsent(
                key,
                v -> {
                  throw new IllegalArgumentException(
                      "Unsupported transformation: "
                          + input.getClass().getName()
                          + ":"
                          + resultType);
                });
    return function.apply(input, context);
  }

  private void initContactTransformers() {
    cache.put(new TransformKey(LinkedHashMap.class, VCard.class), new ToVCardTransformer());
    cache.put(
        new TransformKey(LinkedHashMap.class, Address.class), new ToVCardAddressTransformer());
    cache.put(new TransformKey(VCard.class, LinkedHashMap.class), new ToGraphContactTransformer());
    cache.put(
        new TransformKey(Address.class, LinkedHashMap.class), new ToGraphAddressTransformer());
  }

  private void initCalendarTransformers() {
    cache.put(
        new TransformKey(LinkedHashMap.class, CalendarModel.class),
        new ToCalendarModelTransformer());
    cache.put(
        new TransformKey(LinkedHashMap.class, CalendarEventModel.class),
        new ToCalendarEventModelTransformer());
    cache.put(
        new TransformKey(LinkedHashMap.class, CalendarEventModel.CalendarEventTime.class),
        new ToCalendarEventTimeTransformer());
    cache.put(
        new TransformKey(LinkedHashMap.class, CalendarAttendeeModel.class),
        new ToCalendarAttendeeModelTransformer());
    cache.put(
        new TransformKey(CalendarModel.class, LinkedHashMap.class),
        new ToGraphCalendarTransformer());
    cache.put(
        new TransformKey(CalendarEventModel.class, LinkedHashMap.class),
        new ToGraphEventTransformer());
  }

  private class TransformerContextImpl implements TransformerContext {
    private final List<String> problems = new ArrayList<>();
    private final Map<String, String> properties;

    public TransformerContextImpl(Map<String, String> properties) {
      this.properties = properties;
    }

    @Override
    public <T> T transform(Class<T> resultType, Object input) {
      if (input == null) {
        return null; // support null
      }
      return TransformerServiceImpl.this.transform(resultType, input, this);
    }

    @Override
    public void problem(String message) {
      problems.add(message);
    }

    @Override
    public List<String> getProblems() {
      return problems;
    }

    @Override
    public String getProperty(String key) {
      return properties.get(key);
    }

    @Override
    public void setProperty(String key, String value) {
      properties.put(key, value);
    }
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
      return Objects.equals(from, that.from) && Objects.equals(to, that.to);
    }

    @Override
    public int hashCode() {
      return Objects.hash(from, to);
    }
  }
}
