package org.datatransferproject.api;

import com.google.common.base.Preconditions;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import org.datatransferproject.api.action.Action;
import org.datatransferproject.api.action.datatype.DataTypesAction;
import org.datatransferproject.api.action.transfer.CreateTransferAction;
import org.datatransferproject.api.action.transfer.GenerateServiceAuthDataAction;
import org.datatransferproject.api.action.transfer.GetTransferAction;
import org.datatransferproject.api.action.transfer.GetTransferServicesAction;
import org.datatransferproject.api.action.transfer.PrepareImportAuthAction;
import org.datatransferproject.api.action.transfer.StartTransferAction;
import org.datatransferproject.api.auth.PortabilityAuthServiceProviderRegistry;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.config.FlagBindingModule;
import org.datatransferproject.security.AsymmetricKeyGenerator;
import org.datatransferproject.security.RsaSymmetricKeyGenerator;
import org.datatransferproject.security.SymmetricKeyGenerator;
import org.datatransferproject.spi.api.auth.AuthServiceProviderRegistry;
import org.datatransferproject.spi.api.auth.extension.AuthServiceExtension;
import org.datatransferproject.spi.api.token.TokenManager;
import org.datatransferproject.spi.cloud.storage.JobStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import java.util.List;

/** */
public class ApiServicesModule extends FlagBindingModule {
  private final TypeManager typeManager;
  private final JobStore jobStore;
  private final SymmetricKeyGenerator keyGenerator;
  private final List<AuthServiceExtension> authServiceExtensions;
  private final TrustManagerFactory trustManagerFactory;
  private final KeyManagerFactory keyManagerFactory;
  private final TokenManager tokenManager;

  private final ExtensionContext context;

  public ApiServicesModule(
      TypeManager typeManager,
      JobStore jobStore,
      SymmetricKeyGenerator keyGenerator,
      TrustManagerFactory trustManagerFactory,
      KeyManagerFactory keyManagerFactory,
      List<AuthServiceExtension> authServiceExtensions,
      TokenManager tokenManager,
      ExtensionContext context) {
    this.typeManager = typeManager;
    this.jobStore = jobStore;
    this.keyGenerator = keyGenerator;
    this.authServiceExtensions = authServiceExtensions;
    this.trustManagerFactory = trustManagerFactory;
    this.keyManagerFactory = keyManagerFactory;
    this.tokenManager = tokenManager;
    this.context = context;

    if (trustManagerFactory != null || keyManagerFactory != null) {
      Preconditions.checkNotNull(
          trustManagerFactory,
          "If a key manager factory is specified, a trust manager factory must also be provided");
      Preconditions.checkNotNull(
          keyManagerFactory,
          "If a trust manager factory  is specified, a key manager factory must also be provided");
    }
  }

  @Override
  protected void configure() {
    bindFlags(context);

    MapBinder<String, AuthServiceExtension> mapBinder =
        MapBinder.newMapBinder(binder(), String.class, AuthServiceExtension.class);

    authServiceExtensions.forEach(
        authExtension ->
            mapBinder.addBinding(authExtension.getServiceId()).toInstance(authExtension));

    bind(AuthServiceProviderRegistry.class).to(PortabilityAuthServiceProviderRegistry.class);
    bind(SymmetricKeyGenerator.class).toInstance(keyGenerator);
    bind(TypeManager.class).toInstance(typeManager);
    bind(JobStore.class).toInstance(jobStore);
    bind(TokenManager.class).toInstance(tokenManager);

    if (trustManagerFactory != null) {
      bind(TrustManagerFactory.class).toInstance(trustManagerFactory);
    }
    if (keyManagerFactory != null) {
      bind(KeyManagerFactory.class).toInstance(keyManagerFactory);
    }

    bind(AsymmetricKeyGenerator.class).to(RsaSymmetricKeyGenerator.class);

    Multibinder<Action> actionBinder = Multibinder.newSetBinder(binder(), Action.class);
    actionBinder.addBinding().to(DataTypesAction.class);
    actionBinder.addBinding().to(GetTransferServicesAction.class);
    actionBinder.addBinding().to(CreateTransferAction.class);
    actionBinder.addBinding().to(GenerateServiceAuthDataAction.class);
    actionBinder.addBinding().to(GetTransferAction.class);
    actionBinder.addBinding().to(PrepareImportAuthAction.class);
    actionBinder.addBinding().to(StartTransferAction.class);
  }
}
