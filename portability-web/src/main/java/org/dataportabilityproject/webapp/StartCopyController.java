package org.dataportabilityproject.webapp;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.dataModels.ContinuationInformation;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.dataModels.ExportInformation;
import org.dataportabilityproject.dataModels.Exporter;
import org.dataportabilityproject.dataModels.Importer;
import org.dataportabilityproject.dataModels.Resource;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.auth.AuthData;
import org.dataportabilityproject.webapp.job.JobManager;
import org.dataportabilityproject.webapp.job.PortabilityJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller to initiate the copy. */
@RestController
public class StartCopyController {
  @Autowired
  private ServiceProviderRegistry registry;

  @Autowired
  private JobManager jobManager;

  /** Starts the copy and returns any status information */
  @CrossOrigin(origins = "http://localhost:3000")
  @RequestMapping("/_/startCopy")
  public Map<String, String> fetchCopyConfiguration(
      @CookieValue(value = "jobToken", required = true) String token) throws Exception {
    // TODO: move to interceptor to redirect
    Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token required");

    // Valid job must be present
    PortabilityJob job = jobManager.findExistingJob(token);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", token);

    String exportService = job.exportService();
    Preconditions
        .checkState(!Strings.isNullOrEmpty(job.exportService()), "Export service is invalid");
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");
    String importService = job.importService();
    Preconditions
        .checkState(!Strings.isNullOrEmpty(job.importService()), "Import service is invalid");
    Preconditions.checkState(job.importAuthData() != null, "Import AuthData is required");

    copyDataType(registry, job);

    return ImmutableMap.<String, String>of("status", "started");
  }


  private static <T extends DataModel> void copyDataType(ServiceProviderRegistry registry, PortabilityJob job)
      throws IOException {
    // Valid data type is required to be set in the job by this point
    PortableDataType dataType = getDataType(job.dataType());
    Exporter<T> exporter = registry.getExporter(job.exportService(), dataType, job.exportAuthData());
    Importer<T> importer = registry.getImporter(job.importService(), dataType, job.importAuthData());
    ExportInformation emptyExportInfo =
        new ExportInformation(Optional.empty(), Optional.empty());
    copy(exporter, importer, emptyExportInfo);
  }

  private static <T extends DataModel> void copy(
      Exporter<T> exporter,
      Importer<T> importer,
      ExportInformation exportInformation) throws IOException {

    // NOTE: order is important bellow, do the import of all the items, then do continuation
    // then do sub resources, this ensures all parents are populated before children get
    // processed.

    T items = exporter.export(exportInformation);
    importer.importItem(items);

    ContinuationInformation continuationInfo = items.getContinuationInformation();
    if (null != continuationInfo) {
      if (null != continuationInfo.getPaginationInformation()) {
        copy(exporter, importer,
            new ExportInformation(
                exportInformation.getResource(),
                Optional.of(continuationInfo.getPaginationInformation())));
      }

      if (continuationInfo.getSubResources() != null) {
        for (Resource resource : continuationInfo.getSubResources()) {
          copy(
              exporter,
              importer,
              new ExportInformation(Optional.of(resource), Optional.empty()));
        }
      }
    }
  }

  /** Parse the data type .*/
  private static PortableDataType getDataType(String dataType) {
    com.google.common.base.Optional<PortableDataType> dataTypeOption = Enums
        .getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type required");
    return dataTypeOption.get();
  }

}
