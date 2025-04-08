package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;
import static org.datatransferproject.types.common.models.DataVertical.BLOBS;
import static org.datatransferproject.types.common.models.DataVertical.CALENDAR;
import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.PHOTOS;
import static org.datatransferproject.types.common.models.DataVertical.SOCIAL_POSTS;
import static org.datatransferproject.types.common.models.DataVertical.VIDEOS;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.cloud.storage.AppCredentialStore;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

class GenericTransferServiceVerticalConfig {
  private final DataVertical vertical;

  @JsonCreator
  public GenericTransferServiceVerticalConfig(
      @JsonProperty(value = "vertical", required = true) DataVertical vertical) {
    this.vertical = vertical;
  }

  public DataVertical getVertical() {
    return vertical;
  }

  @Override
  public int hashCode() {
    return this.vertical.hashCode();
  }
}

class GenericTransferServiceConfig {
  private final String serviceId;
  private final URL endpoint;
  private final Set<GenericTransferServiceVerticalConfig> verticals;

  public GenericTransferServiceConfig(
      @JsonProperty(value = "serviceId", required = true) String serviceId,
      @JsonProperty(value = "endpoint", required = true) URL endpoint,
      @JsonProperty(value = "verticals", required = true)
          List<GenericTransferServiceVerticalConfig> verticals) {
    this.serviceId = serviceId;
    this.endpoint = endpoint;
    this.verticals = new HashSet<>(verticals);
  }

  public String getServiceId() {
    return serviceId;
  }

  public URL getEndpoint() {
    return endpoint;
  }

  public Set<GenericTransferServiceVerticalConfig> getVerticals() {
    return verticals;
  }

  public boolean supportsVertical(DataVertical vertical) {
    return verticals.stream()
        .map(verticalConfig -> verticalConfig.getVertical())
        .collect(Collectors.toSet())
        .contains(vertical);
  }
}

public class GenericTransferExtension implements TransferExtension {
  Map<DataVertical, Importer<?, ?>> importerMap = new HashMap<>();

  @Override
  public boolean supportsService(String service) {
    try {
      TransferServiceConfig config = TransferServiceConfig.getForService(service);
      if (config.getServiceConfig().isEmpty()) {
        return false;
      }
      // Parse failures throw
      parseConfig(config.getServiceConfig().get());
    } catch (IOException e) {
      return false;
    }
    // Found and parsed a valid generic service config for the service
    return true;
  }

  @Override
  public void initialize(ExtensionContext context) {
    JobStore jobStore = context.getService(JobStore.class);
    TransferServiceConfig configuration = context.getService(TransferServiceConfig.class);
    if (configuration.getServiceConfig().isEmpty()) {
      throw new RuntimeException("Empty service configuration");
    }
    GenericTransferServiceConfig serviceConfig;
    try {
      serviceConfig = parseConfig(configuration.getServiceConfig().get());
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Invalid service configuration", e);
    }

    AppCredentialStore appCredentialStore = context.getService(AppCredentialStore.class);
    String serviceNameUpper = serviceConfig.getServiceId().toUpperCase();
    AppCredentials appCredentials;
    try {
      appCredentials =
          appCredentialStore.getAppCredentials(
              format("%s_KEY", serviceNameUpper), format("%s_SECRET", serviceNameUpper));
    } catch (IOException e) {
      throw new RuntimeException(
          format(
              "Failed to get application credentials for %s (%s)",
              serviceNameUpper, serviceConfig.getServiceId()),
          e);
    }

    if (serviceConfig.supportsVertical(BLOBS)) {
      BlobbySerializer serializer = new BlobbySerializer(jobStore);
      importerMap.put(
          BLOBS,
          new GenericFileImporter<>(
              serializer::serialize,
              appCredentials,
              urlAppend(serviceConfig.getEndpoint(), "blobs"),
              jobStore,
              context.getMonitor()));
    }

    if (serviceConfig.supportsVertical(MEDIA)
        // PHOTOS and VIDEOS can be mapped from MEDIA
        || serviceConfig.supportsVertical(PHOTOS)
        || serviceConfig.supportsVertical(VIDEOS)) {
      importerMap.put(
          MEDIA,
          new GenericFileImporter<MediaContainerResource, MediaSerializer.ExportData>(
              MediaSerializer::serialize,
              appCredentials,
              urlAppend(serviceConfig.getEndpoint(), "media"),
              jobStore,
              context.getMonitor()));
    }

    if (serviceConfig.supportsVertical(SOCIAL_POSTS)) {
      importerMap.put(
          SOCIAL_POSTS,
          new GenericImporter<SocialActivityContainerResource, SocialPostsSerializer.ExportData>(
              SocialPostsSerializer::serialize,
              appCredentials,
              urlAppend(serviceConfig.getEndpoint(), "social-posts"),
              context.getMonitor()));
    }

    if (serviceConfig.supportsVertical(CALENDAR)) {
      importerMap.put(
          CALENDAR,
          new GenericImporter<CalendarContainerResource, CalendarSerializer.ExportData>(
              CalendarSerializer::serialize,
              appCredentials,
              urlAppend(serviceConfig.getEndpoint(), "calendar"),
              context.getMonitor()));
    }
  }

  private URL urlAppend(URL base, String suffix) {
    try {
      String path = base.getPath();
      if (!path.endsWith("/")) {
        path += "/";
      }
      path += suffix;
      return base.toURI().resolve(path).toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new RuntimeException("Failed to build URL", e);
    }
  }

  private GenericTransferServiceConfig parseConfig(JsonNode config) throws JsonProcessingException {
    ObjectMapper om = new ObjectMapper();
    return om.treeToValue(config, GenericTransferServiceConfig.class);
  }

  @Override
  public String getServiceId() {
    return "Generic";
  }

  @Override
  public Exporter<?, ?> getExporter(DataVertical transferDataType) {
    throw new UnsupportedOperationException("Generic exporters aren't supported");
  }

  @Override
  public Importer<?, ?> getImporter(DataVertical transferDataType) {
    return importerMap.get(transferDataType);
  }
}
