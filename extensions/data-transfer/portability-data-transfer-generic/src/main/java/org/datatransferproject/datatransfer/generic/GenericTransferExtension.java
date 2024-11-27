package org.datatransferproject.datatransfer.generic;

import static org.datatransferproject.types.common.models.DataVertical.BLOBS;
import static org.datatransferproject.types.common.models.DataVertical.CALENDAR;
import static org.datatransferproject.types.common.models.DataVertical.MEDIA;
import static org.datatransferproject.types.common.models.DataVertical.SOCIAL_POSTS;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;

/**
 * Wrapper to adapt items known to be in temp storage (e.g. BLOB data) into {@link DownloadableItem}
 *
 * <p>It's useful to wrap such items so upstream code can consume either known temp store'd items or
 * items the Importer has to download itself (some MEDIA items) from the same interface.
 */
class CachedDownloadableItem implements DownloadableItem {
  private String cachedId;
  private String name;

  public CachedDownloadableItem(String cachedId, String name) {
    this.cachedId = cachedId;
    this.name = name;
  }

  @Override
  public String getIdempotentId() {
    return cachedId;
  }

  @Override
  public String getFetchableUrl() {
    // 'url' is ID when cached
    return cachedId;
  }

  @Override
  public boolean isInTempStore() {
    return true;
  }

  @Override
  public String getName() {
    return name;
  }
}

public class GenericTransferExtension implements TransferExtension {

  private Map<DataVertical, Importer<?, ?>> importerMap = new HashMap<>();

  @Override
  public void initialize(ExtensionContext context) {
    JobStore jobStore = context.getService(JobStore.class);

    importerMap.put(
        BLOBS,
        new GenericFileImporter<BlobbyStorageContainerResource>(
            BlobbySerializer::serialize, jobStore, context.getMonitor()));

    importerMap.put(
        MEDIA,
        new GenericFileImporter<MediaContainerResource>(
            (container, om) ->
                Stream.concat(
                        container.getAlbums().stream()
                            .map(
                                album ->
                                    new ImportableData(
                                        om.valueToTree(album),
                                        album.getIdempotentId(),
                                        album.getName())),
                        Stream.concat(
                            container.getVideos().stream()
                                .map(
                                    (video) -> {
                                      return new ImportableFileData(
                                          video,
                                          om.valueToTree(video),
                                          video.getIdempotentId(),
                                          video.getName());
                                    }),
                            container.getPhotos().stream()
                                .map(
                                    photo -> {
                                      return new ImportableFileData(
                                          photo,
                                          om.valueToTree(photo),
                                          photo.getIdempotentId(),
                                          photo.getName());
                                    })))
                    .collect(Collectors.toList()),
            jobStore,
            context.getMonitor()));

    importerMap.put(
        SOCIAL_POSTS,
        new GenericImporter<SocialActivityContainerResource>(
            (container, om) ->
                container.getActivities().stream()
                    .map(
                        activity -> {
                          // "actor" is stored at the container level, but isn't repliacted
                          // in the tree of activity, so we should merge it in a metadata field
                          // TODO: Consider a POJO with JSON annotations for mapping this
                          ObjectNode root = JsonNodeFactory.instance.objectNode();
                          ObjectNode metadata = JsonNodeFactory.instance.objectNode();
                          metadata.set(
                              "@type", JsonNodeFactory.instance.textNode("SocialActivityMetadata"));
                          metadata.set("actor", om.valueToTree(container.getActor()));
                          root.set(
                              "@type", JsonNodeFactory.instance.textNode("SocialActivityData"));
                          root.set("metadata", metadata);
                          root.set("activity", om.valueToTree(activity));
                          return new ImportableData(
                              root, activity.getIdempotentId(), activity.getName());
                        })
                    .collect(Collectors.toList()),
            context.getMonitor()));

    importerMap.put(
        CALENDAR,
        new GenericImporter<CalendarContainerResource>(
            (container, om) ->
                Stream.concat(
                        container.getCalendars().stream()
                            .map(
                                calendar ->
                                    new ImportableData(
                                        om.valueToTree(calendar),
                                        calendar.getId(),
                                        calendar.getName())),
                        container.getEvents().stream()
                            .map(
                                event ->
                                    new ImportableData(
                                        om.valueToTree(event),
                                        String.valueOf(event.hashCode()),
                                        event.getTitle())))
                    .collect(Collectors.toList()),
            context.getMonitor()));
  }

  @Override
  public String getServiceId() {
    // TODO: Work out how to make this dynamic, or change the way transfer extensions are loaded
    throw new UnsupportedOperationException("Unimplemented method 'getServiceId'");
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
