package org.dataportabilityproject.webapp;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.job.JobManager;
import org.dataportabilityproject.job.PortabilityJob;
import org.dataportabilityproject.shared.PortableDataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller to initiate the copy. */
@RestController
public class StartCopyController {
  @Autowired
  private ServiceProviderRegistry registry;

  @Autowired
  private JobManager jobManager;

  @Autowired
  private CloudFactory cloudFactory;

  /** Starts the copy and returns any status information */
  @RequestMapping("/_/startCopy")
  public Map<String, String> fetchCopyConfiguration(
      @CookieValue(value = JsonKeys.ID_COOKIE_KEY, required = true) String encodedIdCookie) throws Exception {
    // TODO: move to interceptor to redirect
    Preconditions.checkArgument(!Strings.isNullOrEmpty(encodedIdCookie), "Encoded Id Cookie required");

    // Valid job must be present
    String jobId = JobUtils.decodeId(encodedIdCookie);
    PortabilityJob job = jobManager.findExistingJob(jobId);
    Preconditions.checkState(null != job, "existingJob not found for token: %s", jobId);

    String exportService = job.exportService();
    Preconditions
        .checkState(!Strings.isNullOrEmpty(job.exportService()), "Export service is invalid");
    Preconditions.checkState(job.exportAuthData() != null, "Export AuthData is required");
    String importService = job.importService();
    Preconditions
        .checkState(!Strings.isNullOrEmpty(job.importService()), "Import service is invalid");
    Preconditions.checkState(job.importAuthData() != null, "Import AuthData is required");

    PortableDataType type = getDataType(job.dataType());

    // TODO: Design better threading for new copy tasks with exception handling
    Runnable r = new Runnable() {
      public void run() {
        try {
          PortabilityCopier.copyDataType(registry, type, exportService,
              job.exportAuthData(), importService, job.importAuthData(), job.id());
        } catch (IOException e) {
          System.out.println("copyDataType failed");
          e.printStackTrace();
        } finally {
          cloudFactory.clearJobData(job.id());
        }
      }
    };

    ExecutorService executor = Executors.newCachedThreadPool();
    executor.submit(r);

    return ImmutableMap.<String, String>of("status", "started");
  }

    /** Parse the data type .*/
  private static PortableDataType getDataType(String dataType) {
    com.google.common.base.Optional<PortableDataType> dataTypeOption = Enums
        .getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type required");
    return dataTypeOption.get();
  }
}
