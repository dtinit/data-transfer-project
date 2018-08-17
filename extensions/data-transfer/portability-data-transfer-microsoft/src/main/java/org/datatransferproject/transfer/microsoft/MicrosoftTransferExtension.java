package org.datatransferproject.transfer.microsoft;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.transfer.extension.TransferExtension;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.transfer.microsoft.calendar.MicrosoftCalendarExporter;
import org.datatransferproject.transfer.microsoft.calendar.MicrosoftCalendarImporter;
import org.datatransferproject.transfer.microsoft.contacts.MicrosoftContactsExporter;
import org.datatransferproject.transfer.microsoft.contacts.MicrosoftContactsImporter;
import org.datatransferproject.transfer.microsoft.offline.MicrosoftOfflineDataExporter;
import org.datatransferproject.transfer.microsoft.photos.MicrosoftPhotosExporter;
import org.datatransferproject.transfer.microsoft.photos.MicrosoftPhotosImporter;
import org.datatransferproject.transfer.microsoft.transformer.TransformerService;
import org.datatransferproject.transfer.microsoft.transformer.TransformerServiceImpl;

/** Bootstraps the Microsoft data transfer services. */
public class MicrosoftTransferExtension implements TransferExtension {
  public static final String SERVICE_ID = "microsoft";
  // TODO: centralized place, or enum type for these?
  private static final String CONTACTS = "CONTACTS";
  private static final String CALENDAR = "CALENDAR";
  private static final String PHOTOS = "PHOTOS";
  private static final String OFFLINE_DATA = ""OFFLINE-DATA"";
  private static final String BASE_GRAPH_URL = "https://graph.microsoft.com";

  private final boolean offlineData;

  private boolean initialized = false;

  private JobStore jobStore;

  // Needed for ServiceLoader to load this class.
  public MicrosoftTransferExtension() {
    offlineData = Boolean.parseBoolean(System.getProperty("offlineData"));
  }

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    // TODO consider memoizing w/ Supplier but we will only use once and pass to transfer.
    // This could allow us to refactor these params into supplier.
    OkHttpClient client = new OkHttpClient.Builder().build();
    ObjectMapper mapper = new ObjectMapper();
    TransformerService transformerService = new TransformerServiceImpl();

    if (transferDataType.equals(CONTACTS)) {
      return new MicrosoftContactsExporter(BASE_GRAPH_URL, client, mapper, transformerService);
    }
    if (transferDataType.equals(CALENDAR)) {
      return new MicrosoftCalendarExporter(BASE_GRAPH_URL, client, mapper, transformerService);
    }

    if (transferDataType.equals(PHOTOS)) {
      return new MicrosoftPhotosExporter(BASE_GRAPH_URL, client, mapper, jobStore);
    }

    if (offlineData && transferDataType.equals(OFFLINE_DATA)) {
      // only enable if derivded data explicitly set as a configuration value
      // TODO we may want to provide a config option that allows deployers to disable transfer of certain data types
      return new MicrosoftOfflineDataExporter(BASE_GRAPH_URL, client, mapper);
    }

    return null;
  }

  @Override
  public Importer<?, ?> getImporter(String transferDataType) {
    OkHttpClient client = new OkHttpClient.Builder().build();
    ObjectMapper mapper = new ObjectMapper();
    TransformerService transformerService = new TransformerServiceImpl();

    if (transferDataType.equals(CONTACTS)) {
      return new MicrosoftContactsImporter(BASE_GRAPH_URL, client, mapper, transformerService);
    }
    if (transferDataType.equals(CALENDAR)) {
      return new MicrosoftCalendarImporter(
          BASE_GRAPH_URL, client, mapper, transformerService, jobStore);
    }

    if (transferDataType.equals(PHOTOS)) {
      return new MicrosoftPhotosImporter(BASE_GRAPH_URL, client, mapper, jobStore);
    }

    return null;
  }

  @Override
  public void initialize(ExtensionContext context) {
    // Note: initialize could be called twice in an account migration scenario where we import and
    // export to the same service provider. So just return rather than throwing if called multiple
    // times.
    if (initialized) return;
    jobStore = context.getService(JobStore.class);
    initialized = true;
  }
}
