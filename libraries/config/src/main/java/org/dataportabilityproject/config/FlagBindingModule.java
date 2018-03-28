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

package org.dataportabilityproject.config;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.api.launcher.Flag;
import org.dataportabilityproject.config.extension.SettingsExtension;

/**
 * {@link AbstractModule} that binds all @Flag-annotated flags from an {@link ExtensionContext}
 * as Named annotations to be injected by Guice.
 */
public class FlagBindingModule extends AbstractModule {
  private final ExtensionContext context;

  public FlagBindingModule(ExtensionContext context) {
    this.context = context;
  }

  @Override
  protected void configure() {
    bindFlags();
  }

  @SuppressWarnings("unchecked")
  private void bindFlags() {
    // Automatically bind all flags in our ExtensionContext to a Named annotation. e.g., binds:
    // settingsExtension.cloud() to a String annotated with @Named("cloud"), so core classes
    // may inject '@Named("cloud") String cloud'.
    for (Method method : context.getClass().getMethods()) {
      boolean isFlagMethod = method.isAnnotationPresent(Flag.class);
      if (!isFlagMethod) {
        continue;
      }

      Class returnType = method.getReturnType();
      Object flagValue;
      try {
        flagValue = method.invoke(context);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException("Could not get flag value by invoking: " + method.getName(), e);
      }
      Preconditions.checkNotNull(flagValue, "Required flag " + method.getName() + " was mull");
      bind(returnType).annotatedWith(Names.named(method.getName())).toInstance(flagValue);
    }
  }
}
