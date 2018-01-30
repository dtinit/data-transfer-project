package org.dataportabilityproject.cloud.microsoft.cosmos;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.RemoteEndpointAwareJdkSSLOptions;
import com.datastax.driver.core.SSLOptions;
import com.datastax.driver.core.Session;

import javax.net.ssl.SSLContext;

/**
 * Configures a client connection to a Cassandra cluster.
 */
public class CassandraCluster {
    private Cluster cluster;
    private String host = "127.0.0.1";
    private int port = 10350;
    private String username = "localhost";
    private String password = "defaultpassword";
    private String keyStorePassword = "changeit";


    /**
     * Creates a session from the cluster configuration.
     *
     * @param ssl true if the session should use SSL.
     */
    public Session createSession(boolean ssl) {
        try {
            Cluster.Builder builder = Cluster.builder();
            if (ssl) {
                SSLContext sslContext = SSLContext.getDefault();
                SSLOptions sslOptions = RemoteEndpointAwareJdkSSLOptions.builder().withSSLContext(sslContext).build();
                builder.withSSL(sslOptions);
            }
            cluster = builder.addContactPoint(host).withPort(port).withCredentials(username, password).build();

            return cluster.connect();
        } catch (Exception e) {
            throw new MicrosoftStorageException("Error creating connection to Cassandra", e);
        }
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void close() {
        if (cluster != null) {
            cluster.close();
        }
    }

    public static class Builder {
        private CassandraCluster cassandraCluster;

        public static Builder newInstance() {
            return new Builder();
        }

        public Builder host(String host) {
            cassandraCluster.host = host;
            return this;
        }

        public Builder port(int port) {
            cassandraCluster.port = port;
            return this;
        }

        public Builder username(String username) {
            cassandraCluster.username = username;
            return this;
        }

        public Builder password(String password) {
            cassandraCluster.password = password;
            return this;
        }

        public CassandraCluster build() {
            return cassandraCluster;
        }

        private Builder() {
            cassandraCluster = new CassandraCluster();
        }
    }

}
