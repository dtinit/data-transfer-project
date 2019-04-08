package org.datatransferproject.api.launcher;

import java.util.HashMap;
import java.util.Map;

public class DelegatingExtensionContext implements ExtensionContext {
  private final Map<Class<?>, Object> overriddenRegisteredClasses = new HashMap<>();
  private final ExtensionContext baseExtensionContext;

  public DelegatingExtensionContext(ExtensionContext baseExtensionContext) {
    this.baseExtensionContext = baseExtensionContext;
  }
  @Override
  public TypeManager getTypeManager() {
    return baseExtensionContext.getTypeManager();
  }

  @Override
  public Monitor getMonitor() {
    return baseExtensionContext.getMonitor();
  }

  @Override
  public <T> void registerService(Class<T> type, T service) {
    baseExtensionContext.registerService(type, service);
  }

  @Override
  public <T> T getService(Class<T> type) {
    if (overriddenRegisteredClasses.containsKey(type)) {
      return type.cast(overriddenRegisteredClasses.get(type));
    }
    return baseExtensionContext.getService(type);
  }

  @Override
  public <T> T getSetting(String setting, T defaultValue) {
    return baseExtensionContext.getSetting(setting, defaultValue);
  }

  @Override
  public String cloud() {
    return baseExtensionContext.cloud();
  }

  @Override
  public Constants.Environment environment() {
    return baseExtensionContext.environment();
  }

  public <T> void registerOverrideService(Class<T> type, T service){
    overriddenRegisteredClasses.put(type, service);
  }
}
