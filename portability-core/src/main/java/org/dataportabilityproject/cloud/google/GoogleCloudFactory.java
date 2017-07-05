package org.dataportabilityproject.cloud.google;


import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import java.io.IOException;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.interfaces.JobDataCache;
import org.dataportabilityproject.cloud.interfaces.PersistentKeyValueStore;
import org.dataportabilityproject.shared.Secrets;

public final class GoogleCloudFactory implements CloudFactory {

  private final Datastore datastore;

  public GoogleCloudFactory(Secrets secrets) {
    try {
      this.datastore = DatastoreOptions
          .newBuilder()
          .setProjectId(secrets.get("GOOGLE_PROJECT_ID"))
          .setCredentials(GoogleCredentials.fromStream(
              secrets.getReferencedInputStream("GOOGLE_DATASTORE_CREDS_FILE")
          ))
          .build()
          .getService();
    } catch (IOException e) {
      throw new IllegalStateException("Couldn't create datastore", e);
    }
  }

  @Override
  public JobDataCache getJobDataCache(String jobId, String service) {
    return new GoogleJobDataCache(datastore, jobId, service);
  }

  @Override
  public PersistentKeyValueStore getPersistentKeyValueStore() {
    return new GooglePersistentKeyValueStore(datastore);
  }

  @Override
  public void clearJobData(String jobId) {
    QueryResults<Key> results = datastore.run(Query.newKeyQueryBuilder()
        .setKind(GoogleJobDataCache.USER_KEY_KIND)
        .setFilter(PropertyFilter.hasAncestor(
            datastore.newKeyFactory().setKind(GoogleJobDataCache.JOB_KIND).newKey(jobId)))
        .build());
    results.forEachRemaining(datastore::delete);
  }
}
