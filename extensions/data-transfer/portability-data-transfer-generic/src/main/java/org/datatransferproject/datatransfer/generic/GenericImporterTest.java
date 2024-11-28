package org.datatransferproject.datatransfer.generic;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import org.datatransferproject.api.launcher.Constants.Environment;
import org.datatransferproject.api.launcher.ExtensionContext;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.api.launcher.TypeManager;
import org.datatransferproject.launcher.monitor.ConsoleMonitor;
import org.datatransferproject.launcher.monitor.ConsoleMonitor.Level;
import org.datatransferproject.spi.cloud.storage.JobStore;
import org.datatransferproject.spi.cloud.types.PortabilityJob;
import org.datatransferproject.spi.cloud.types.PortabilityJob.State;
import org.datatransferproject.spi.transfer.idempotentexecutor.InMemoryIdempotentImportExecutor;
import org.datatransferproject.spi.transfer.provider.Importer;
import org.datatransferproject.types.common.models.DataVertical;
import org.datatransferproject.types.common.models.blob.BlobbyStorageContainerResource;
import org.datatransferproject.types.common.models.blob.DigitalDocumentWrapper;
import org.datatransferproject.types.common.models.blob.DtpDigitalDocument;
import org.datatransferproject.types.common.models.calendar.CalendarAttendeeModel;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.datatransferproject.types.common.models.media.MediaAlbum;
import org.datatransferproject.types.common.models.media.MediaContainerResource;
import org.datatransferproject.types.common.models.photos.PhotoModel;
import org.datatransferproject.types.common.models.social.SocialActivityActor;
import org.datatransferproject.types.common.models.social.SocialActivityAttachment;
import org.datatransferproject.types.common.models.social.SocialActivityAttachmentType;
import org.datatransferproject.types.common.models.social.SocialActivityContainerResource;
import org.datatransferproject.types.common.models.social.SocialActivityLocation;
import org.datatransferproject.types.common.models.social.SocialActivityModel;
import org.datatransferproject.types.common.models.social.SocialActivityType;
import org.datatransferproject.types.common.models.videos.VideoModel;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.datatransferproject.types.transfer.errors.ErrorDetail;
import org.datatransferproject.types.transfer.serviceconfig.TransferServiceConfig;

class TestJobStore implements JobStore {

  @Override
  public InputStreamWrapper getStream(UUID jobId, String key) throws IOException {
    Random random = new Random();
    byte[] data = new byte[1024 * 1024 * 50];
    random.nextBytes(data);
    return new InputStreamWrapper(new ByteArrayInputStream(data));
  }

  @Override
  public void createJob(UUID jobId, PortabilityJob job) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'createJob'");
  }

  @Override
  public void claimJob(UUID jobId, PortabilityJob job) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'claimJob'");
  }

  @Override
  public void updateJobAuthStateToCredsAvailable(UUID jobId) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException(
        "Unimplemented method 'updateJobAuthStateToCredsAvailable'");
  }

  @Override
  public void updateJobWithCredentials(UUID jobId, PortabilityJob job) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'updateJobWithCredentials'");
  }

  @Override
  public void addErrorsToJob(UUID jobId, Collection<ErrorDetail> errors) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'addErrorsToJob'");
  }

  @Override
  public void addFailureReasonToJob(UUID jobId, String failureReason) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'addFailureReasonToJob'");
  }

  @Override
  public void markJobAsFinished(UUID jobId, State state) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'markJobAsFinished'");
  }

  @Override
  public void markJobAsStarted(UUID jobId) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'markJobAsStarted'");
  }

  @Override
  public void markJobAsTimedOut(UUID jobId) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'markJobAsTimedOut'");
  }

  @Override
  public void remove(UUID jobId) throws IOException {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'remove'");
  }

  @Override
  public PortabilityJob findJob(UUID jobId) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findJob'");
  }

  @Override
  public UUID findFirst(org.datatransferproject.spi.cloud.types.JobAuthorization.State jobState) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'findFirst'");
  }
}

class TestExtensionContext implements ExtensionContext {
  private Monitor monitor;
  private JobStore jobStore = new TestJobStore();

  TestExtensionContext(Monitor monitor) {
    this.monitor = monitor;
  }

  @Override
  public TypeManager getTypeManager() {
    throw new UnsupportedOperationException("Unimplemented method 'getTypeManager'");
  }

  @Override
  public Monitor getMonitor() {
    return monitor;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T getService(Class<T> type) {
    if (type.getName() == JobStore.class.getName()) {
      return (T) jobStore;
    }
    if (type.getName() == TransferServiceConfig.class.getName()) {
      InputStream inputStream =
          this.getClass().getClassLoader().getResourceAsStream("config/test.yaml");
      try {
        return (T) TransferServiceConfig.create(inputStream);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    throw new RuntimeException(format("Unexpected type %s", type.getName()));
  }

  @Override
  public <T> T getSetting(String setting, T defaultValue) {
    throw new UnsupportedOperationException("Unimplemented method 'getSetting'");
  }

  @Override
  public String cloud() {
    throw new UnsupportedOperationException("Unimplemented method 'cloud'");
  }

  @Override
  public Environment environment() {
    throw new UnsupportedOperationException("Unimplemented method 'environment'");
  }
}

public class GenericImporterTest {
  // TODO: Make this in to an actual unit test
  public static void main(String args[]) throws Exception {
    ConsoleMonitor monitor = new ConsoleMonitor(Level.DEBUG);

    InMemoryIdempotentImportExecutor idempotentImporter =
        new InMemoryIdempotentImportExecutor(monitor);
    TestExtensionContext context = new TestExtensionContext(monitor);
    GenericTransferExtension extension = new GenericTransferExtension();
    extension.initialize(context);
    UUID jobId = UUID.randomUUID();
    idempotentImporter.setJobId(jobId);
    monitor.info(() -> format("Supports test: %s", extension.supportsService("test")));

    @SuppressWarnings("unchecked")
    Importer<TokensAndUrlAuthData, SocialActivityContainerResource> socialImporter =
        (Importer<TokensAndUrlAuthData, SocialActivityContainerResource>)
            extension.getImporter(DataVertical.SOCIAL_POSTS);
    socialImporter.importItem(
        jobId,
        idempotentImporter,
        new TokensAndUrlAuthData("foo", "bar", "baz"),
        new SocialActivityContainerResource(
            "123",
            new SocialActivityActor("321", "Steve", null),
            Arrays.asList(
                new SocialActivityModel(
                    "456",
                    Instant.now(),
                    SocialActivityType.NOTE,
                    Arrays.asList(
                        new SocialActivityAttachment(
                            SocialActivityAttachmentType.IMAGE, "foo.com", "Foo", null)),
                    new SocialActivityLocation("foo", 10, 10),
                    "Hello world!",
                    "Hi there",
                    null))));

    @SuppressWarnings("unchecked")
    Importer<TokensAndUrlAuthData, CalendarContainerResource> calendarImporter =
        (Importer<TokensAndUrlAuthData, CalendarContainerResource>)
            extension.getImporter(DataVertical.CALENDAR);
    calendarImporter.importItem(
        jobId,
        idempotentImporter,
        new TokensAndUrlAuthData("foo", "bar", "baz"),
        new CalendarContainerResource(
            Arrays.asList(new CalendarModel("calendar123", "Calendar 123", "Calendar description")),
            Arrays.asList(
                new CalendarEventModel(
                    "calendar123",
                    "Event 1",
                    "Event notes",
                    Arrays.asList(
                        new CalendarAttendeeModel("attendee1", "attendee1@example.com", false),
                        new CalendarAttendeeModel("attendee2", "attendee2@example.com", true)),
                    "Event Place",
                    new CalendarEventModel.CalendarEventTime(OffsetDateTime.now(), false),
                    new CalendarEventModel.CalendarEventTime(OffsetDateTime.now(), false),
                    null))));

    @SuppressWarnings("unchecked")
    Importer<TokensAndUrlAuthData, BlobbyStorageContainerResource> fileImporter =
        (Importer<TokensAndUrlAuthData, BlobbyStorageContainerResource>)
            extension.getImporter(DataVertical.BLOBS);

    fileImporter.importItem(
        jobId,
        idempotentImporter,
        new TokensAndUrlAuthData("foo", "bar", "baz"),
        new BlobbyStorageContainerResource(
            "root",
            "rootdir",
            new ArrayList<>(),
            Arrays.asList(
                new BlobbyStorageContainerResource(
                    "foo",
                    "foodir",
                    Arrays.asList(
                        new DigitalDocumentWrapper(
                            new DtpDigitalDocument("bar.mp4", "2020-02-01", "video/mp4"),
                            "video/mp4",
                            "foobarfile")),
                    new ArrayList<>()))));

    @SuppressWarnings("unchecked")
    Importer<TokensAndUrlAuthData, MediaContainerResource> mediaImporter =
        (Importer<TokensAndUrlAuthData, MediaContainerResource>)
            extension.getImporter(DataVertical.MEDIA);
    mediaImporter.importItem(
        jobId,
        idempotentImporter,
        new TokensAndUrlAuthData("foo", "bar", "baz"),
        new MediaContainerResource(
            Arrays.asList(new MediaAlbum("album123", "Album 123", "Album description")),
            Arrays.asList(
                new PhotoModel(
                    "bar.jpeg",
                    "cachedVal2",
                    "Bar description",
                    "image/jpeg",
                    "cachedVal2",
                    "album123",
                    true,
                    null,
                    new Date())),
            Arrays.asList(
                new VideoModel(
                    "foo.mp4",
                    "cachedVal1",
                    "Foo description",
                    "video/mp4",
                    "cachedVal1",
                    "album123",
                    true,
                    new Date()))));
  }
}
