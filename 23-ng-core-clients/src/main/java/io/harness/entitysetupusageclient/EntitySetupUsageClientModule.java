package io.harness.entitysetupusageclient;

import static io.harness.annotations.dev.HarnessTeam.DX;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageClient;
import io.harness.entitysetupusageclient.remote.EntitySetupUsageHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

@OwnedBy(DX)
public class EntitySetupUsageClientModule extends AbstractModule {
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;

  public EntitySetupUsageClientModule(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
  }

  @Provides
  private EntitySetupUsageHttpClientFactory entityReferenceHttpClientFactory(
      KryoConverterFactory kryoConverterFactory) {
    return new EntitySetupUsageHttpClientFactory(
        ngManagerClientConfig, serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory);
  }

  @Override
  protected void configure() {
    bind(EntitySetupUsageClient.class).toProvider(EntitySetupUsageHttpClientFactory.class).in(Scopes.SINGLETON);
  }
}
