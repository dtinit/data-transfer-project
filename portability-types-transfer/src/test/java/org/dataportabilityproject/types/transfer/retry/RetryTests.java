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

package org.dataportabilityproject.types.transfer.retry;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.junit.Test;

public class RetryTests {

  @Test
  public void testSerialize() throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    NoRetryStrategy fatalStrategy = new NoRetryStrategy();
    RetryMapping fatalMapping = new RetryMapping(new String[] {"status code: 4\\d\\d"}, fatalStrategy);
    SimpleRetryStrategy simpleRetryStrategy = new SimpleRetryStrategy(5, 100);
    RetryMapping simpleMapping = new RetryMapping(new String[] {"simple"}, simpleRetryStrategy);
    ExponentialBackoffStrategy exponentialBackoffStrategy = new ExponentialBackoffStrategy(5, 20,
        1.5);

    RetryStrategyLibrary library = new RetryStrategyLibrary(
        ImmutableList.of(fatalMapping, simpleMapping), exponentialBackoffStrategy);

    String yaml = mapper.writeValueAsString(library);
    System.out.println("yaml: \n" + yaml);
  }
}
