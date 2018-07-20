# Integration Guide

This guide covers the following options for integrating your service, with the primary use case being the integration of a new transfer service.

* [Integrate a new transfer service](#integrate-a-new-transfer-service)
* [Integrate a new data model](#integrate-a-new-data-model)
* [Integrate a new cloud provider](#integrate-a-new-cloud-provider)

## Integrate a new transfer service
A new service can be integrated into the Data Transfer Project by creating the following extensions:

1. Transfer Extension
1. Auth Extension
1. Optionally, a new Data Model may be required if your does not exist. Please see Integrate a new Data Model (below) for more information.

### Transfer Extension

Transfer Extensions are located in the [extensions/data-transfer module](https://github.com/google/data-transfer-project/tree/master/extensions/data-transfer), where they are organized by service provider and then by data type.

1. Create a new directory for your provider under extensions/data-transfer

  `$mkdir extensions/data-transfer/portability-data-transfer-foo`

2. Create a build.gradle file in your new directory

  `$touch extensions/data-transfer/portability-data-transfer-foo/build.gradle`

3. Add the spi and cloud dependencies to your build file along with any sdk or related dependencies specific to your code

  * Common dependencies include logging, see example below

  ```javascript
  /*
   *  Copyright 2018 The Data Transfer Project Authors.
   * 
   *  Licensed under the Apache License, Version 2.0 (the "License");
   *  you may not use this file except in compliance with the License.
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

  dependencies {
      compile project(':portability-spi-cloud')
      compile project(':portability-spi-transfer')
      
     // logging
      compile("org.slf4j:slf4j-api:${slf4jVersion}")
      compile("org.slf4j:slf4j-log4j12:${slf4jVersion}")
     
      compile(YOUR DEPS HERE)
      }
  ```
  
4. Add an entry for your extension in root settings.gradle
``` javascript
// Foo transfer extension
include ':extensions:data-transfer:portability-data-transfer-foo'
```

5. Create the META-INF file

  `$touch extensions/data-transfer/portability-data-transfer-foo/src/main/resources/META-INF/services/org.dataportabilityproject.spi.transfer.extension.TransferExtension`

6. Create the directory for your main code

 `$mkdir -p extensions/data-transfer/portability-data-transfer-foo/src/main/java/org/dataportabilityproject/transfer/foo/`

7. Create FooTransferExtension extending TransferExtension

  * See class javadoc for [TransferExtension](https://github.com/google/data-transfer-project/blob/master/portability-spi-transfer/src/main/java/org/datatransferproject/spi/transfer/extension/TransferExtension.java) for full documentation

  * Best practices:

    * Validate the TransferExtension isn’t initialized more than once in a running binary
    * Most providers will interact with their api via a ‘registered’ app providing a client id/key and secret. Those will be stored in a key management system and retrieved via an identifier for the key and one for the secret,
    * In initialize, retrieve the AppCredentialStore
    * Return an Exporter and Importer for a given transfer type and mode

8. Create FooTransferExtensionTest
  * See existing directories for tests

### Auth Extension

Auth Extensions are located in the [extensions/auth module](https://github.com/google/data-transfer-project/tree/master/extensions/auth), where they are organized by service provider.  Note: This structure is subject to change sooner rather than later.

1. See class javadoc for the [AuthServiceExtension](https://github.com/google/data-transfer-project/blob/master/portability-spi-api/src/main/java/org/datatransferproject/spi/api/auth/extension/AuthServiceExtension.java) for full documentation
  * Best practices:
    * Validate the AuthServiceExtension isn’t initialized more than once in a running binary
    * Most providers will interact with their api via a ‘registered’ app providing a client id/key and secret. Those will be stored in a key management system and retrieved via an identifier for the key and one for the secret,
    * In initialize, retrieve the AppCredentialStore
    * Return an FooAuthDataGenerator for a given transfer type and mode

2. Create an FooAuthDataGenerator implementing AuthDataGenerator
  * This class should implement the generateConfiguration() method, which will generate the url and related data to redirect the user to for authentication, e.g. the first part of an Oauth step.
  * This class should also implement the generateAuthData() method, which will take the token obtained in the first step and execute the necessary steps to obtain an AuthData object containing tokens to execute requests with, e.g. Oauth access tokens.

3. Create the test directory for the FooAuthDataGeneratorTest

  * `$mkdir -p extensions/auth/portability-auth-instagram/src/test/java/org/datatransferproject/auth/foo`

4. Create FooAuthDataGeneratorTest with appropriate tests



## Integrate a new Data Model

* The shared model classes for supported Transfer Data Types are located in the [portability-types-transfer](https://github.com/google/data-transfer-project/tree/master/portability-types-transfer) module

* The first thing to do is look for any existing model classes that may support your use case or that can be extended to support your use case

* If you would like to add a new Data Model, please send out a high level proposal or semi complete code to dtp-discuss@googlegroups.com

## Integrate a new cloud provider

* The spi interfaces to be implemented by are located in the [portability-spi-cloud](https://github.com/google/data-transfer-project/tree/master/portability-spi-cloud) module.  See documentation in that folder and classes.

* See an example of necessary plugin points in the [GCP folder](https://github.com/google/data-transfer-project/tree/master/distributions/demo-google-deployment)

* If you would like to add a new Cloud Provider, please send out a high level proposal or semi complete code to dtp-discuss@googlegroups.com
