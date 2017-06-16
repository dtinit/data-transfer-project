package org.dataportabilityproject.serviceProviders.instagram;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.shared.auth.OfflineAuthDataGenerator;
import org.dataportabilityproject.shared.auth.SecretAuthData;

public final class InstagramServiceProvider implements ServiceProvider {
  private final InastagramAuth inastagramAuth;

  public InstagramServiceProvider(Secrets secrets) {
    this.inastagramAuth = new InastagramAuth(
        secrets.get("INSTAGRAM_CLIENT_ID"),
        secrets.get("INSTAGRAM_CLIENT_SECRET"));
  }

  @Override
  public String getName() {
    return "Instagram";
  }

  @Override
  public ImmutableList<PortableDataType> getExportTypes() {
    return ImmutableList.of(PortableDataType.PHOTOS);
  }

  @Override
  public ImmutableList<PortableDataType> getImportTypes() {
    // Currently Instagram doesn't support import.
    return ImmutableList.of();
  }

  @Override
  public OfflineAuthDataGenerator getOfflineAuthDataGenerator(PortableDataType dataType) {
    return inastagramAuth;
  }

  @Override
  public Exporter<? extends DataModel> getExporter(PortableDataType type, AuthData authData)
      throws IOException {
    if (type == PortableDataType.PHOTOS) {
      return new InstagramPhotoService(((SecretAuthData) authData).secret());
    }
    throw new IllegalStateException("Instagram doesn't support exporting: " + type);
  }

  @Override
  public Importer<? extends DataModel> getImporter(PortableDataType type, AuthData authData)
      throws IOException {
    throw new IllegalStateException("Instagram doesn't support importing anything");
  }
}
