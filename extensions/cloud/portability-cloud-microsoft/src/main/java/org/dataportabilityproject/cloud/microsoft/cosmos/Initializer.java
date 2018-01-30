package org.dataportabilityproject.cloud.microsoft.cosmos;

import com.datastax.driver.core.Session;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.dataportabilityproject.cloud.microsoft.cosmos.MicrosoftCloudConstants.DATA_TABLE;
import static org.dataportabilityproject.cloud.microsoft.cosmos.MicrosoftCloudConstants.JOB_TABLE;
import static org.dataportabilityproject.cloud.microsoft.cosmos.MicrosoftCloudConstants.KEY_SPACE;

/**
 * Creates and initializes a {@link CosmosStore} instance. Supports Azure and local setup.
 */
public class Initializer {

    /**
     * Returns a new {@link CosmosStore} instance configured for Azure.
     */
    public CosmosStore get(ObjectMapper mapper) {
        CassandraCluster.Builder builder = CassandraCluster.Builder.newInstance();
        // TODO configure builder
        CassandraCluster cassandraCluster = builder.build();
        Session session = cassandraCluster.createSession(true);

        createKeyspace(session);
        createTables(session);

        return new CosmosStore(session, mapper);
    }

    /**
     * Returns a new {@link CosmosStore} instance configured for local use.
     */
    public CosmosStore getLocal(int port, ObjectMapper mapper) {
        CassandraCluster.Builder builder = CassandraCluster.Builder.newInstance();
        builder.port(port);
        CassandraCluster cassandraCluster = builder.build();
        Session session = cassandraCluster.createSession(false);
        createKeyspace(session);
        createTables(session);
        return new CosmosStore(session, mapper);
    }

    private void createKeyspace(Session session) {
        String query = "CREATE KEYSPACE IF NOT EXISTS " + KEY_SPACE + " WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'datacenter1' : 1 }";
        session.execute(query);
    }

    private void createTables(Session session) {
        String jobTableQuery = "CREATE TABLE IF NOT EXISTS " + JOB_TABLE + " (job_id uuid PRIMARY KEY, job_data text)";
        session.execute(jobTableQuery);
        String dataTableQuery = "CREATE TABLE IF NOT EXISTS " + DATA_TABLE + " (data_id uuid  PRIMARY KEY, data_model text)";
        session.execute(dataTableQuery);
    }


}
