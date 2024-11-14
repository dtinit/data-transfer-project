package org.datatransferproject.datatransfer.generic;

import static org.datatransferproject.types.common.models.DataVertical.SOCIAL_POSTS;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.DownloadableItem;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;

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
