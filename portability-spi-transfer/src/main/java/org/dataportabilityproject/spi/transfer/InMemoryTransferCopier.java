package org.dataportabilityproject.spi.transfer;

import java.io.IOException;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProvider;
import org.dataportabilityproject.spi.transfer.provider.TransferServiceProviderRegistry;
import org.dataportabilityproject.types.transfer.PortableType;
import org.dataportabilityproject.types.transfer.auth.AuthData;
import org.dataportabilityproject.types.transfer.models.DataModel;

/**
 * In-memory Copier interface
 */
public interface InMemoryTransferCopier {
  /* Copies the provided dataType from exportService to importService */
  void copyDataType(TransferServiceProviderRegistry registry,
      PortableType dataType,
      String exportService,
      AuthData exportAuthData,
      String importService,
      AuthData importAuthData,
      String jobId) throws IOException;
}
