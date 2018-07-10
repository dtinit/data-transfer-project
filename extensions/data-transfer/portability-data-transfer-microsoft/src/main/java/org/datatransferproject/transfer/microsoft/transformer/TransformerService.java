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

import java.util.Map;

/** Transforms instances into different types. */
public interface TransformerService {

  /**
   * Transform the input instance into an instance of a result type.
   *
   * @param resultType the type to transform to
   * @param input the input instance
   */
  <T> TransformResult<T> transform(Class<T> resultType, Object input);

  /**
   * Transform the input instance into an instance of a result type.
   *
   * @param resultType the type to transform to
   * @param input the input instance
   * @param properties the context properties
   */
  <T> TransformResult<T> transform(
      Class<T> resultType, Object input, Map<String, String> properties);
}
