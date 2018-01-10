/*
* Copyright 2017 The Data-Portability Project Authors.
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
package org.dataportabilityproject.webapp;


import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import com.sun.net.httpserver.HttpHandler;
import org.dataportabilityproject.PortabilityCoreModule;
import org.dataportabilityproject.job.PortabilityJobFactory;
import org.dataportabilityproject.job.UUIDProvider;
public class PortabilityApiModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new PortabilityCoreModule());
    bind(PortabilityJobFactory.class).toInstance(new PortabilityJobFactory(new UUIDProvider()));

    MapBinder<String, HttpHandler> mapbinder
        = MapBinder.newMapBinder(binder(), String.class, HttpHandler.class);


    // HttpServer does exact longest matching prefix for context matching. This means
    // /_/listServices, /_/listServicesthisshouldnotwork and /_/listServices/path/to/resource will
    // all be handled by the ListServicesHandler below. To prevent this, each handler below should
    // validate the request URI that it is getting passed in.
    mapbinder.addBinding(CopySetupHandler.PATH).to(CopySetupHandler.class);
    mapbinder.addBinding(ImportSetupHandler.PATH).to(ImportSetupHandler.class);

    mapbinder.addBinding("/_/listDataTypes").to(ListDataTypesHandler.class);
    mapbinder.addBinding("/_/listServices").to(ListServicesHandler.class);
    mapbinder.addBinding("/_/startCopy").to(StartCopyHandler.class);
    mapbinder.addBinding("/callback/").to(Oauth2CallbackHandler.class);
    mapbinder.addBinding("/callback1/").to(OauthCallbackHandler.class);
    mapbinder.addBinding("/configure").to(ConfigureHandler.class);
    mapbinder.addBinding("/simpleLoginSubmit").to(SimpleLoginSubmitHandler.class);
  }
}
