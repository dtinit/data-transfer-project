/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.job;

import com.google.auto.value.AutoValue;
import com.google.common.base.Converter;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.Map;
import javax.annotation.Nullable;
import org.dataportabilityproject.job.JobDao.JobState;
import org.dataportabilityproject.shared.auth.AuthData;

/** Data about a particular portability job. */
@AutoValue
public abstract class PortabilityJob {
  public abstract String id();
  @Nullable public abstract String dataType();
  @Nullable public abstract String exportService();
  @Nullable public abstract String exportAccount();
  @Nullable public abstract AuthData exportInitialAuthData();
  /** @deprecated Use encryptedExportAuthData when encrypted flow is implemented. */
  @Deprecated @Nullable public abstract AuthData exportAuthData();
  @Nullable public abstract String encryptedExportAuthData();
  @Nullable public abstract String importService();
  @Nullable public abstract String importAccount();
  @Nullable public abstract AuthData importInitialAuthData();
  /** @deprecated Use encryptedImportAuthData when encrypted flow is implemented. */
  @Deprecated @Nullable public abstract AuthData importAuthData();
  @Nullable public abstract String encryptedImportAuthData();
  @Nullable public abstract String sessionKey();
  @Nullable public abstract String workerInstancePublicKey();
  @Nullable public abstract String workerInstancePrivateKey(); // TODO: Consider removing
  @Nullable public abstract JobState jobState();

  public static Builder builder() {
     return new AutoValue_PortabilityJob.Builder();
  }

  abstract Builder toBuilder();

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setId(String id);
    public abstract Builder setDataType(String id);
    public abstract Builder setExportService(String id);
    public abstract Builder setExportAccount(String id);
    public abstract Builder setExportInitialAuthData(AuthData id);
    /** @deprecated Use setEncryptedExportAuthData when encrypted flow is implemented. */
    @Deprecated public abstract Builder setExportAuthData(AuthData id);
    public abstract Builder setEncryptedExportAuthData(String id);
    public abstract Builder setImportService(String id);
    public abstract Builder setImportAccount(String id);
    public abstract Builder setImportInitialAuthData(AuthData id);
    /** @deprecated Use setEncryptedImportAuthData when encrypted flow is implemented. */
    @Deprecated public abstract Builder setImportAuthData(AuthData id);
    public abstract Builder setEncryptedImportAuthData(String id);
    public abstract Builder setSessionKey(String id);
    public abstract Builder setWorkerInstancePublicKey(String id);
    public abstract Builder setWorkerInstancePrivateKey(String id);
    public abstract Builder setJobState(JobState jobState);

    abstract PortabilityJob autoBuild(); // not public

    /** Validates required values on build. */
    public PortabilityJob build() {
      PortabilityJob job = autoBuild();
      Preconditions.checkState(!Strings.isNullOrEmpty(job.id()), "Invalid id value");
      return job;
    }
  }

  /** Represents this job as Map of key value pairs. */
  public Map<String, Object> asMap() {
    return new PortabilityJobConverter().doForward(this);
  }

  /** Creates a {@link PortabilityJob} from the data in the given {@code map}. */
  public static PortabilityJob mapToJob(Map<String, Object> map) {
    return new PortabilityJobConverter().doBackward(map);
  }
}
