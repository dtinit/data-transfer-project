package org.dataportabilityproject.worker;

import org.dataportabilityproject.job.*;

import com.google.common.base.Enums;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.dataportabilityproject.ConsoleIO;
import org.dataportabilityproject.PortabilityCopier;
import org.dataportabilityproject.ServiceProviderRegistry;
import org.dataportabilityproject.cloud.interfaces.CloudFactory;
import org.dataportabilityproject.cloud.local.LocalCloudFactory;
import org.dataportabilityproject.dataModels.DataModel;
import org.dataportabilityproject.shared.IOInterface;
import org.dataportabilityproject.shared.PortableDataType;
import org.dataportabilityproject.shared.Secrets;
import org.dataportabilityproject.shared.auth.AuthData;

/** Main class to bootstrap a portabilty worker that will operate on a single job. */
public class WorkerMain {
  private static CloudFactory cloudFactory = new LocalCloudFactory();

  public static void main(String[] args) throws Exception {

    // TODO: Worker should only request secrets for the services it needs
    Secrets secrets = new Secrets("secrets.csv");
    ServiceProviderRegistry registry = new ServiceProviderRegistry(secrets, cloudFactory);

    // Pick up session id and session public key from custom value in startup script

    // Create instance key

    // Encrypt instance key with session public key

    // Store encrypted instance key in DB

    // Poll data store session id for updated encrypted creds
  }

  /** Parse the data type .*/
  private static PortableDataType getDataType(String dataType) {
    com.google.common.base.Optional<PortableDataType> dataTypeOption = Enums
        .getIfPresent(PortableDataType.class, dataType);
    Preconditions.checkState(dataTypeOption.isPresent(), "Data type required");
    return dataTypeOption.get();
  }
}
