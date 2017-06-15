package org.dataportabilityproject.serviceProviders.instagram;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.ServiceProvider;
import org.dataportabilityproject.shared.auth.SecretAuthData;

public final class InstagramServiceProvider implements ServiceProvider {
  private final Secrets secrets;

  public InstagramServiceProvider(Secrets secrets) {
    this.secrets = secrets;
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
  public Exporter<? extends DataModel> getExporter(PortableDataType type) throws IOException {
    if (type == PortableDataType.PHOTOS) {
      InastagramAuth inastagramAuth = new InastagramAuth(
          secrets.get("INSTAGRAM_CLIENT_ID"),
          secrets.get("INSTAGRAM_CLIENT_SECRET"));
      SecretAuthData authData = (SecretAuthData) inastagramAuth.generateAuthData(null);
      return new InstagramPhotoService(authData.secret());
    }
    throw new IllegalStateException("Instagram doesn't support exporting: " + type);
  }

  @Override
  public Importer<? extends DataModel> getImporter(PortableDataType type) throws IOException {
    throw new IllegalStateException("Instagram doesn't support importing anything");
  }
}
