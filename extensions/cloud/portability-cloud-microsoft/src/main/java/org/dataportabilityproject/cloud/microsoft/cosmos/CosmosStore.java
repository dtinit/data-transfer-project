/*
 * Copyright 2018 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataportabilityproject.cloud.microsoft.cosmos;

import static org.dataportabilityproject.cloud.microsoft.cosmos.MicrosoftCloudConstants.DATA_TABLE;
import static org.dataportabilityproject.cloud.microsoft.cosmos.MicrosoftCloudConstants.JOB_TABLE;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.transfer.models.DataModel;

/**
 * A {@link JobStore} backed by Cosmos DB. This implementation uses the DataStax Cassandra driver to
 * communicate with Cosmos DB.
 */
public class CosmosStore implements JobStore {
  static final String JOB_INSERT =
      String.format("INSERT INTO  %s (job_id, job_data) VALUES (?,?)", JOB_TABLE);
  static final String JOB_QUERY = String.format("SELECT * FROM %s WHERE job_id = ?", JOB_TABLE);
  static final String JOB_DELETE = String.format("DELETE FROM %s WHERE job_id = ?", JOB_TABLE);
  static final String JOB_UPDATE =
      String.format("UPDATE %s SET job_data = ? WHERE job_id = ?", JOB_TABLE);

  static final String DATA_INSERT =
      String.format("INSERT INTO  %s (data_id, data_model) VALUES (?,?)", DATA_TABLE);
  static final String DATA_QUERY = String.format("SELECT * FROM %s WHERE data_id = ?", DATA_TABLE);
  static final String DATA_DELETE = String.format("DELETE FROM %s WHERE data_id = ?", DATA_TABLE);
  static final String DATA_UPDATE =
      String.format("UPDATE %s SET data_model = ? WHERE data_id = ?", DATA_TABLE);

  private final Session session;
  private final ObjectMapper mapper;

  public CosmosStore(Session session, ObjectMapper mapper) {
    this.session = session;
    this.mapper = mapper;
  }

  public void close() {
    if (session != null) {
      session.close();
    }
  }

  @Override
  public void createJob(UUID jobId, PortabilityJob job) {
    create(jobId, job, JOB_INSERT);
  }

  @Override
  public void updateJob(UUID jobId, PortabilityJob job) {
    Preconditions.checkNotNull(jobId, "Job not persisted");
    update(jobId, job, JOB_UPDATE);
  }

  @Override
  public void updateJob(UUID jobId, PortabilityJob job, JobUpdateValidator validator)
      throws IOException {
    // TODO: if validator != null, call validator.validate() as part of update transaction. See
    // https://github.com/google/data-transfer-project/pull/187/files/ac5b796988aa70c2e8d6948f17c59a025ae1947a#r173193724
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public PortabilityJob findJob(UUID id) {
    return findData(PortabilityJob.class, id, JOB_QUERY, "job_data");
  }

  @Override
  public void remove(UUID id) {
    remove(id, JOB_DELETE);
  }

  @Override
  public <T extends DataModel> void create(UUID jobId, T model) {
    create(jobId, model, DATA_INSERT);
  }

  @Override
  public <T extends DataModel> void update(UUID jobId, T model) {
    update(jobId, model, DATA_UPDATE);
  }

  @Override
  public <T extends DataModel> T findData(Class<T> type, UUID id) {
    return findData(type, id, DATA_QUERY, "data_model");
  }

  @Override
  public void removeData(UUID id) {
    remove(id, DATA_DELETE);
  }

  @Override
  public void create(UUID jobId, String key, InputStream stream) {
    // TODO implement with Azure Blob Storage
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public InputStream getStream(UUID jobId, String key) {
    // TODO implement with Azure Blob Storage
    throw new UnsupportedOperationException("Not yet implemented");
  }

  @Override
  public UUID findFirst(JobAuthorization.State jobState) {
    throw new UnsupportedOperationException();
  }

  private void create(UUID id, Object instance, String query) {
    PreparedStatement statement = session.prepare(query);
    BoundStatement boundStatement = new BoundStatement(statement);
    try {
      boundStatement.setUUID(0, id);
      boundStatement.setString(1, mapper.writeValueAsString(instance));
      session.execute(boundStatement);
    } catch (JsonProcessingException e) {
      throw new MicrosoftStorageException("Error creating data: " + id, e);
    }
  }

  private void update(UUID id, Object instance, String query) {
    PreparedStatement statement = session.prepare(query);
    BoundStatement boundStatement = new BoundStatement(statement);
    try {
      boundStatement.setString(0, mapper.writeValueAsString(instance));
      boundStatement.setUUID(1, id);
      session.execute(boundStatement);
    } catch (JsonProcessingException e) {
      throw new MicrosoftStorageException("Error deleting data: " + id, e);
    }
  }

  private <T> T findData(Class<T> type, UUID id, String query, String column) {
    PreparedStatement statement = session.prepare(query);
    BoundStatement boundStatement = new BoundStatement(statement);
    boundStatement.bind(id);

    Row row = session.execute(boundStatement).one();
    String serialized = row.getString(column);
    try {
      return mapper.readValue(serialized, type);
    } catch (IOException e) {
      throw new MicrosoftStorageException("Error deserializing data: " + id, e);
    }
  }

  private void remove(UUID id, String query) {
    PreparedStatement statement = session.prepare(query);
    BoundStatement boundStatement = new BoundStatement(statement);
    boundStatement.setUUID(0, id);
    session.execute(boundStatement);
  }
}
