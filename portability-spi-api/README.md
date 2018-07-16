# portability-spi-api

This folder contains the Service Provider Interface (SPI) for DTP's
API server.

Service providers looking to be included in DTP will need to implement
[AuthDataGenerator](src/main/java/org/datatransferproject/spi/api/auth/AuthDataGenerator.java)
and [AuthServiceExtension](src/main/java/org/datatransferproject/spi/api/auth/extension/AuthServiceExtension.java).

