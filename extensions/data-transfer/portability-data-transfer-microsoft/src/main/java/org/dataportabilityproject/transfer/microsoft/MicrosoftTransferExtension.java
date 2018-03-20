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
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerService;
import org.dataportabilityproject.transfer.microsoft.transformer.TransformerServiceImpl;

public class MicrosoftTransferExtension implements TransferExtension {
  public static final String SERVICE_ID = "microsoft";
  // TODO: centralized place, or enum type for these?
  private static final String CONTACTS = "contacts";
  private static final String CALENDAR = "calendar";
  private static final String BASE_GRAPH_URL = "https://graph.microsoft.com";

  private boolean initialized = false;

  private JobStore jobStore;

  // Needed for ServiceLoader to load this class.
  public MicrosoftTransferExtension() {}

  @Override
  public String getServiceId() {
    return SERVICE_ID;
  }

  @Override
  public Exporter<?, ?> getExporter(String transferDataType) {
    // TODO consider memoizing w/ Supplier but we will only use once and pass to worker.
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
