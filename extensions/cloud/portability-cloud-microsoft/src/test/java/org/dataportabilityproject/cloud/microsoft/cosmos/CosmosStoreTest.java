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

import static org.dataportabilityproject.cloud.microsoft.cosmos.CosmosStore.JOB_DELETE;
import static org.dataportabilityproject.cloud.microsoft.cosmos.CosmosStore.JOB_INSERT;
import static org.dataportabilityproject.cloud.microsoft.cosmos.CosmosStore.JOB_QUERY;
import static org.dataportabilityproject.cloud.microsoft.cosmos.CosmosStore.JOB_UPDATE;
import static org.scassandra.cql.PrimitiveType.UUID;
import static org.scassandra.cql.PrimitiveType.VARCHAR;
import static org.scassandra.matchers.Matchers.preparedStatementRecorded;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.Map;
import org.dataportabilityproject.spi.cloud.types.JobAuthorization.State;
import org.dataportabilityproject.spi.cloud.types.PortabilityJob;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.scassandra.Scassandra;
import org.scassandra.ScassandraFactory;
import org.scassandra.http.client.PreparedStatementExecution;
import org.scassandra.http.client.PrimingRequest;
import org.scassandra.http.client.types.ColumnMetadata;

/**
 * Verifies the CosmosStore implementation against a mock Cassandra instance.
 */
public class CosmosStoreTest {
    private CosmosStore cosmosStore;
    private Scassandra cassandra;

    @Test
    @Ignore
    public void verifyCreateAndFind() throws Exception {
        PrimingRequest.Then.ThenBuilder thenInsert = PrimingRequest.then();
        thenInsert.withVariableTypes(UUID, VARCHAR);
        PrimingRequest createRequest = PrimingRequest.preparedStatementBuilder()
            .withQuery(JOB_INSERT).withThen(thenInsert).build();

        cassandra.primingClient().prime(createRequest);

        PortabilityJob primeJob = PortabilityJob.builder().build();
        java.util.UUID jobId = java.util.UUID.randomUUID();
        Map row =
            Collections.singletonMap("job_data", new ObjectMapper().writeValueAsString(primeJob));

        PrimingRequest.Then.ThenBuilder thenQuery = PrimingRequest.then();
        PrimingRequest findRequest = PrimingRequest.preparedStatementBuilder()
                .withQuery(JOB_QUERY)
                .withThen(thenQuery.withVariableTypes(UUID)
                    .withColumnTypes(ColumnMetadata.column("job_id", UUID)).withRows(row)).build();

        cassandra.primingClient().prime(findRequest);

        PrimingRequest.Then.ThenBuilder thenUpdate = PrimingRequest.then();
        thenUpdate.withVariableTypes(VARCHAR, UUID).withColumnTypes(
            ColumnMetadata.column("job_data", VARCHAR), ColumnMetadata.column("job_id", UUID));
        PrimingRequest updateRequest = PrimingRequest.preparedStatementBuilder()
                .withQuery(JOB_UPDATE)
                .withThen(thenUpdate)
                .build();

        cassandra.primingClient().prime(updateRequest);

        PrimingRequest.Then.ThenBuilder thenRemove = PrimingRequest.then();
        thenRemove.withVariableTypes(UUID);
        PrimingRequest removeRequest = PrimingRequest.preparedStatementBuilder()
            .withQuery(JOB_DELETE).withThen(thenRemove).build();
        cassandra.primingClient().prime(removeRequest);

        PortabilityJob createJob = PortabilityJob.builder().build();
        cosmosStore.createJob(jobId, createJob);

        PortabilityJob copy = cosmosStore.findJob(jobId).toBuilder()
            .setState(PortabilityJob.State.COMPLETE).build();
        cosmosStore.updateJob(jobId, copy);

        cosmosStore.remove(jobId);

        PreparedStatementExecution expectedStatement = PreparedStatementExecution.builder()
                .withPreparedStatementText(JOB_DELETE)
                .withConsistency("LOCAL_ONE")
                .build();

        Assert.assertThat(cassandra.activityClient().retrievePreparedStatementExecutions(),
            preparedStatementRecorded(expectedStatement));
    }

    @Before
    @Ignore
    public void setUp() {
        cassandra = ScassandraFactory.createServer();
        cassandra.start();

        int port = cassandra.getBinaryPort();
        ObjectMapper mapper = new ObjectMapper();
        cosmosStore = new LocalCosmosStoreInitializer().createLocalStore(port, mapper);
    }
}