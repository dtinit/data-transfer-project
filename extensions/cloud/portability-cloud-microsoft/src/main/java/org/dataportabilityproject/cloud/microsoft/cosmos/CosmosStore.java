/*
 * Copyright 2018 The Data-Portability Project Authors.
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

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dataportabilityproject.spi.cloud.storage.JobStore;
import org.dataportabilityproject.spi.cloud.types.LegacyPortabilityJob;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.dataportabilityproject.types.transfer.models.DataModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static org.dataportabilityproject.cloud.microsoft.cosmos.MicrosoftCloudConstants.DATA_TABLE;
import static org.dataportabilityproject.cloud.microsoft.cosmos.MicrosoftCloudConstants.JOB_TABLE;

/**
 * A {@link JobStore} backed by Cosmos DB. This implementation uses the DataStax Cassandra driver to communicate with Cosmos DB.
 */
public class CosmosStore implements JobStore {
    static final String JOB_INSERT = String.format("INSERT INTO  %s (job_id, job_data) VALUES (?,?)", JOB_TABLE);
    static final String JOB_QUERY = String.format("SELECT * FROM %s WHERE job_id = ?", JOB_TABLE);
    static final String JOB_DELETE = String.format("DELETE FROM %s WHERE job_id = ?", JOB_TABLE);
    static final String JOB_UPDATE = String.format("UPDATE %s SET job_data = ? WHERE job_id = ?", JOB_TABLE);

    static final String DATA_INSERT = String.format("INSERT INTO  %s (data_id, data_model) VALUES (?,?)", DATA_TABLE);
    static final String DATA_QUERY = String.format("SELECT * FROM %s WHERE data_id = ?", DATA_TABLE);
    static final String DATA_DELETE = String.format("DELETE FROM %s WHERE data_id = ?", DATA_TABLE);
    static final String DATA_UPDATE = String.format("UPDATE %s SET data_model = ? WHERE data_id = ?", DATA_TABLE);

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
    public void createJob(PortabilityJob job) {
        if (job.getId() != null) {
            throw new IllegalStateException("Job already created: " + job.getId());
        }
        UUID uuid = UUID.randomUUID();
        job.setId(uuid.toString());
        create(uuid, job, JOB_INSERT);
    }

    @Override
    public void updateJob(PortabilityJob job) {
        if (job.getId() == null) {
            throw new IllegalStateException("Job not persisted: " + job.getId());
        }
        update(job.getId(), job, JOB_UPDATE);
    }

    @Override
    public PortabilityJob findJob(String id) {
        return findData(PortabilityJob.class, id, JOB_QUERY, "job_data");
    }

    @Override
    public void remove(String id) {
        remove(id, JOB_DELETE);
    }

    @Override
    public <T extends DataModel> void create(String jobId, T model) {
        create(UUID.fromString(jobId), model, DATA_INSERT);
    }

    @Override
    public <T extends DataModel> void update(String jobId, T model) {
        update(jobId, model, DATA_UPDATE);
    }

    @Override
    public <T extends DataModel> T findData(Class<T> type, String id) {
        return findData(type, id, DATA_QUERY, "data_model");
    }

    @Override
    public void removeData(String id) {
        remove(id, DATA_DELETE);
    }

    @Override
    public void create(String jobId, String key, InputStream stream) {
        // TODO implement with Azure Blob Storage
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public InputStream getStream(String jobId, String key) {
        // TODO implement with Azure Blob Storage
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void create(LegacyPortabilityJob job) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void update(LegacyPortabilityJob job, LegacyPortabilityJob.JobState previousState) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public LegacyPortabilityJob find(String jobId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String findFirst(LegacyPortabilityJob.JobState jobState) {
        throw new UnsupportedOperationException();
    }

    @Override
    public LegacyPortabilityJob find(String jobId, LegacyPortabilityJob.JobState jobState) {
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

    private void update(String id, Object instance, String query) {
        PreparedStatement statement = session.prepare(query);
        BoundStatement boundStatement = new BoundStatement(statement);
        try {
            boundStatement.setString(0, mapper.writeValueAsString(instance));
            boundStatement.setUUID(1, UUID.fromString(id));
            session.execute(boundStatement);
        } catch (JsonProcessingException e) {
            throw new MicrosoftStorageException("Error deleting data: " + id, e);
        }
    }

    private <T> T findData(Class<T> type, String id, String query, String column) {
        PreparedStatement statement = session.prepare(query);
        BoundStatement boundStatement = new BoundStatement(statement);
        boundStatement.bind(UUID.fromString(id));

        Row row = session.execute(boundStatement).one();
        String serialized = row.getString(column);
        try {
            return mapper.readValue(serialized, type);
        } catch (IOException e) {
            throw new MicrosoftStorageException("Error deserializing data: " + id, e);
        }
    }

    private void remove(String id, String query) {
        PreparedStatement statement = session.prepare(query);
        BoundStatement boundStatement = new BoundStatement(statement);
        boundStatement.setUUID(0, UUID.fromString(id));
        session.execute(boundStatement);
    }


}
