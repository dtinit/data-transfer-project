package org.dataportabilityproject.transfer.microsoft;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.dataportabilityproject.api.launcher.ExtensionContext;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.transfer.extension.TransferExtension;
import org.dataportabilityproject.spi.transfer.provider.Exporter;
import org.dataportabilityproject.spi.transfer.provider.Importer;
import org.dataportabilityproject.transfer.microsoft.calendar.MicrosoftCalendarExporter;
import org.dataportabilityproject.transfer.microsoft.calendar.MicrosoftCalendarImporter;
import org.dataportabilityproject.transfer.microsoft.contacts.MicrosoftContactsExporter;
import org.dataportabilityproject.transfer.microsoft.contacts.MicrosoftContactsImporter;
import org.dataportabilityproject.transfer.microsoft.derived.MicrosoftDerivedDataExporter;
import org.dataportabilityproject.transfer.microsoft.photos.MicrosoftPhotosExporter;
import org.dataportabilityproject.transfer.microsoft.photos.MicrosoftPhotosImporter;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerServiceImpl;

/**
 * Bootstraps the Microsoft data transfer services. Note the format of the exported contents are
 * opaque; they may change without notice.
 */
public class MicrosoftTransferExtension implements TransferExtension {
  public static final String SERVICE_ID = "microsoft";
  // TODO: centralized place, or enum type for these?
  private static final String CONTACTS = "contacts";
  private static final String CALENDAR = "calendar";
  private static final String PHOTOS = "photos";
  private static final String DERIVED_DATA = "derived-data";
  private static final String BASE_GRAPH_URL = "https://graph.microsoft.com";

  private final boolean derivedData;

  private boolean initialized = false;

  private JobStore jobStore;

  // Needed for ServiceLoader to load this class.
  public MicrosoftTransferExtension() {
    derivedData = Boolean.parseBoolean(System.getProperty("derivedData"));
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

    if (derivedData && transferDataType.equals(DERIVED_DATA)) {
      return new MicrosoftDerivedDataExporter(BASE_GRAPH_URL, client, mapper);
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
