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

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.RemoteEndpointAwareJdkSSLOptions;
import com.datastax.driver.core.SSLOptions;
import com.datastax.driver.core.Session;
import javax.net.ssl.SSLContext;

/** Configures a client connection to a Cassandra cluster. */
public class CassandraCluster {
  private Cluster cluster;
  // TODO externalize in configuration
  private String host = "127.0.0.1";
  // TODO externalize in configuration
  private int port = 10350;
  // TODO externalize in configuration
  private String username = "localhost";
  // TODO externalize in configuration
  private String password = "defaultpassword";

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
        SSLOptions sslOptions =
            RemoteEndpointAwareJdkSSLOptions.builder().withSSLContext(sslContext).build();
        builder.withSSL(sslOptions);
      }
      cluster =
          builder.addContactPoint(host).withPort(port).withCredentials(username, password).build();

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

    private Builder() {
      cassandraCluster = new CassandraCluster();
    }

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
  }
}
