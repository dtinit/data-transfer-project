# Integration Guide

This guide covers the following options for integrating your service, with the primary use case being the integration of a new transfer service.

* Integrate a new transfer service
* Integrate a new data type
* Integrate a new cloud provider

## Integrate a new transfer service
A new service can be integrated into the Data Transfer Project by creating a 1) Transfer Extension and 2) Auth Extension.
 
### Transfer Extension
* Transfer Extensions are located in the `extensions/data-transfer` [module](https://github.com/google/data-transfer-project/tree/master/extensions/data-transfer), where they are organized by service provider and then by data type.
* _TODO: complete this documentation_

### Auth Extension
* Auth Extensions are located in the `extensions/auth` [module](https://github.com/google/data-transfer-project/tree/master/extensions/auth), where they are organized by service provider.
* _TODO: complete this documentation_

## Integrate a new Data Type

 * The shared model classes for supported Transfer Data Types are located in the `portability-types-transfer` [module](https://github.com/google/data-transfer-project/tree/master/portability-types-transfer/src/main/java/org/dataportabilityproject/types/transfer/models)
 * The first thing to do is look for any existing model classes that may support your use case or that can be extended to support your use case
 * If a new model is required, _TODO: complete this documentation_
 
 ## Integrate a new cloud provider
 
 _TODO: complete this documentation_
 
   
