# Integration Guide

This guide covers the following options for integration your service, with the primary use case being the integration of a new transfer service

* Integrate a new transfer service
* Integrate a new data type
* Integrate a new cloud provider

## Integrate a new transfer service
A new service can be integrated into the Data Transfer Project by creating an 1) Auth Extension and 2) Transfer Extension
 
### Transfer Extension
* Transfer Extensions are located in the extensions [package](https://github.com/google/data-transfer-project/tree/master/extensions/data-transfer) 
* _TODO: complete this documentation_

### Auth Extension
* Auth Extensions are located in the extensions [package](https://github.com/google/data-transfer-project/tree/master/extensions/auth) 
* _TODO: complete this documentation_

## Integrate a new Data Type

 * The shared model classes for supported Transfer Data Types are located in the package [portability-types-transfer](https://github.com/google/data-transfer-project/tree/master/portability-types-transfer/src/main/java/org/dataportabilityproject/types/transfer/models)
 * The first thing to do is look for any existing model classes that may support your use case or that can be extended to support your use case
 * If a new model is required, _TODO: complete this documentation_
 
 ## Integrate a new cloud provider
 
 _TODO: complete this documentation_
 
   
