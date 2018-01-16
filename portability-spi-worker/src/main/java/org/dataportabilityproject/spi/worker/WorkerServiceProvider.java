package org.dataportabilityproject.spi.worker;

/**
 * Factory responsible for providing {@link Exporter} and {@link Importer} implementations that transfer data from and to a particular service.
 */
public interface WorkerServiceProvider {

    /**
     * Returns the id of the service this factory supports.
     */
    String getServiceId();

    /**
     * Provides an exporter for the given transfer data type
     *
     * @param transferDataType the transfer data tyoe
     */
    Exporter<?, ?> getExporter(String transferDataType);

    /**
     * Provides an importer for the given transfer data type
     *
     * @param transferDataType the transfer data tyoe
     */
    Importer<?, ?> getImporter(String transferDataType);

}
