# Data Transfer Project Overview
2018-July

## Vision

Users should be able to easily transfer their data directly between online service providers.

The organizations involved with this project are developing tools that can convert any service's proprietary APIs to and from a small set of standardized data formats that can be used by anyone. This makes it possible to transfer data between any two providers using existing industry-standard infrastructure and authorization mechanisms, such as OAuth. So far, we have developed adapters for seven different service providers across five different types of consumer data; we think this demonstrates the viability of this approach to scale to a large number of use cases.

Consumers will benefit from improved flexibility and control over their data. They will be able to import their information into any participating service that offers compelling features—even brand new ones that could rely on powerful, cloud-based infrastructure rather than the consumers’ potentially limited bandwidth and capability to transfer files. Services will benefit as well, as they will be able to compete for users that can move their data more easily.

## Guiding principles

### Build for users
Data portability tools should be easy to find, intuitive to use, and readily available for individuals. They should also be open and interoperable with standard industry formats, where applicable, so that individuals can easily transfer data between services or download it for their own purposes.

### Privacy and security   
Providers on each side of the portability transaction should have strong privacy and security measures---such as encryption in transit---to guard against unauthorized access, diversion of data, or other types of fraud. It is important to apply privacy principles such as data minimization and transparency when transferring data between Providers. Individuals should be told in a clear and concise manner about the types and scope of data being transferred as well as how the data will be used at the destination service. Individuals should also be advised about the privacy and security practices of the destination service. These measures will help to educate an individual about the data being transferred and how the data will be used at the destination service.

### Reciprocity  
While portability offers more choice and flexibility for individuals, it will be important to guard against incentives that are misaligned with the individual's interests. An individual’s decision to move data to another service should not result in any loss of transparency or control over that data. Individuals should have assurance that data imported to a Provider can likewise be exported again, if they so choose.

### User data, not company data
Data portability is not, and should not be, without reasonable limits. Portability efforts should be limited to data that has utility for an individual: specifically, content an individual creates, imports, approves for collection, or has control over. This reduces the friction for individuals who want to switch among products or services because the data they export is meaningful to them. Portability should not extend to data collected to make a service operate, including data generated to improve system performance or train models that may be commercially sensitive or proprietary. This approach also encourages companies to continue to support data portability, knowing that their proprietary technologies are not threatened by data portability requirements.

### Respect everyone
We live in a collaborative world: people connect and share on social media, they edit docs together, and they comment on videos, pictures and more. Data portability tools should focus only on providing data that is directly tied to the person requesting the transfer. We think this strikes the right balance between portability, privacy and benefits of trying a new service.

## Benefits of Portability

### Prevent lock-in  
Individuals should be able to choose the service that best meets their needs, and not be stuck with the first service they used just because it holds all their data and they are afraid of losing it.

### Foster innovation
If new products can easily have access to existing user data, it should lower the bar for entry into a market.

### Promote competition  
Services won't be able to rest on past successes if individuals can easily transfer data to new services. This should hopefully increase competition in the marketplace, both from existing services and new services.

## Example/Targeted use cases

### Switching to a new service  
Individuals should be able to move completely from one service to another.  For example, if an individual wants to move from Google Drive to Office 365, they should be able to easily copy all of their documents to Office 365, and then delete all of their old documents in Google Drive.

### Trying out a new service  
Individuals should be able to try out new services.  For example, an individual hears about a new email service with an amazing new UI, but it's hard to try it out without significant amounts of real data in it.  The individual should be able to easily make a copy of all their old email, import it into the new service and try it out.  Then they can decide whether to keep their email in one or both of the services.

### Moving selected data  
Individuals should be able to move selected parts of their data to new companies.  For example, an individual may keep all of their photos in Google Photos, but wants to make a photo book with a different service.  They should be able to move just the desired pictures to the photobook service without having to use significant bandwidth to first download, then upload, the photos.

## Architecture

The DTP solution was designed so that it is easy for a service provider to adopt and enable the standards, and took into consideration the following constraints:

### Use existing standards
By supporting existing standards where possible (like OAuth and REST), we aim to minimize the foundational work required before DTP can be built and put into action. Widespread adoption and understanding of existing standards makes this possible. As new standards are developed and adopted, they will be reviewed and, where applicable, incorporated into DTP (see the alternatives section). 

### Minimize the work to add a service
We designed DTP to allow Providers to participate without impacting their own core infrastructure. Providers can build Adapters and enable import and export functionality that works with their existing APIs and Authorization mechanisms. 

### Support standard deployment infrastructure
DTP infrastructure was designed with the flexibility to be hosted by anyone, anywhere.

## Top level components

The project is broken out into five main pieces: Data Models, Adapters, Task Management Library, Hosting Platform and User Interface.

It is important that all ecosystem partners work off of the same Data Models and Adapters.  However the system is designed so that the Task Management, Cloud Interface, and UI can all be swapped out as needed.

### Data models

A Data Model is used to describe a Vertical's file type and associated metadata. For example, in the Photos vertical, a Data Model would include a file type (.jpg, .tiff) and the associated metadata needed to fully describe the .jpg as a photo, such as title, description and album.

### Adapters  

Adapters exist outside of a Provider’s core infrastructure and provide the translation between a Provider’s core infrastructure and the DTP environment. 
-  Data Adapters translate a Provider’s APIs into Data Models, and vice versa
-  Authentication Adapters translate from a Provider’s Authentication to the DTP Authentication

### Task management library

The Task Management Library is a set of system components designed to coordinate and manage tasks that export and import data including retrieving data, pagination when applicable, handling exceptions utilizing retry strategies, re-creation of folder/container structures, and importing of data.

### Hosting platform

The technical environment where a DTP instance can be hosted. This can be a cloud environment, enterprise infrastructure, or local. As of July 2018, the supported cloud hosting platforms include Google Cloud Platform and Microsoft Azure.

### User interface

The UI layer that talks to the task management layer to manage data transfer jobs.

## Deployment

DTP code resides in a centralized, open source repository, and is intended to work for most use cases.  However, contributors are encouraged to fork and extend the project to meet their specific needs.

Service providers interested in enabling data import/export with DTP will want to set up and run a copy of the hosting platform service (optionally with custom UI, Cloud Interface, and Task Management library components).

The preferred scenario is that the system works as a pull model: if an individual is trying to move their data from service A to service B, the individual would initiate the transfer via service B. However it seems likely that in the case where an individual wants to move from LargeCorp to TinyStartup, TinyStartup might not have yet set up a full Hosting Platform to power the move, and in that case LargeCorp would allow the transfer to be started from their service.

### Alternatives considered
Another option is for an entity to run a centralized, common instance/UI that allows transfers between any two service providers in the system.  The benefit is that it would be easy to brand/market with a central solution, have a good user experience, be easy to enforce reciprocity, and be easier to manage API keys.  The disadvantage is that it requires lots of coordination between the service providers, could be difficult to establish a cost sharing model, and would require validation to the integrity of the hosting entity.

## Detailed component design

### Data specifications

Representations of common data attributes for each data type providing for a common set of data attributes that are portable across service providers.

Where standards exist, the model will encapsulate the standard.

Example model for data type "Photo" vertical:

```java
class PhotosModelWrapper implements DataModel {  
  Collection<PhotoAlbum> albums;  
  Collection<PhotoModel> photos;  
  ContinuationInformation continuationInformation;  
}

class PhotoAlbum {  
  String id;  
  String name;  
  String description;  
}

class PhotoModel {  
  String title;  
  String fetchableUrl;  
  String description;  
  String mediaType;  
  String albumId;
}
```

For a given vertical there doesn't need to be only one data specification, but there should be no more than a handful.

### Adapter plugin model

Each adapter needs to provide 3 pieces of functionality: authorization, export, and import.

Authorization is ideally provided by OAuth, in which case a common auth code can be reused, but adapters are free to define arbitrary auth protocols as needed.

The [export interface](https://github.com/google/data-transfer-project/blob/master/portability-spi-transfer/src/main/java/org/datatransferproject/spi/transfer/provider/Exporter.java) can be implemented to allow each service's adapter code to retrieve data and return it in a common data model format.  It also allows for continuation and pagination of data:

```java
public interface Exporter<A extends AuthData, T extends DataModel> {
  ExportResult<T> export(UUID jobId, A authData, Optional<ExportInformation> exportInformation)
      throws Exception;
}
```
ExportInformation contains a ContainerResource and PaginationData.  A ContainerResource represents a collection (such as an album or playlist) so that it is clear which collection subsequent data should be added to.  PaginationData contains information about what the next page is - for example, if the job is currently looking at page 2 of a collection, then PaginationData should direct to page 3.

The [import interface](https://github.com/google/data-transfer-project/blob/master/portability-spi-transfer/src/main/java/org/datatransferproject/spi/transfer/provider/Importer.java) allows for implementations to import or upload the data stored in a common data model format, which may include a collection of data or single data item:

```java
public interface Importer<A extends AuthData, T extends DataModel> {
  ImportResult importItem(UUID jobId, A authData, T data) throws Exception;
}
```

### Task management

This component handles starting and managing data transfer jobs.  This includes things like persisting state, handling pagination, managing retries and rate limiting (globally, per service, and per user).  One class used for this is the JobDataCache, which can be used for storing information that should be used across multiple instances.

Individual deployments are free to use any task management implementation they choose, however the reference implementation is abstracted away from the cloud layer so that providers can provide the cloud implementation of their choice (Azure, AWS, GCP, etc.) to make this as reusable as possible.


### Hosting platform

Because service providers will likely want to run DTP in a cloud environment, DTP is designed to abstract out the specific cloud implementation and only rely on features available in all the major platforms.

Aside from the ability to run docker images, the only [requirements](https://github.com/google/data-transfer-project/blob/master/portability-spi-cloud/src/main/java/org/datatransferproject/spi/cloud/extension/CloudExtension.java) for running a hosting platform are:

-  JobStore - a persistent key-value store to manage transfer job-related data
-  AppCredentialStore - secure storage of application credentials, e.g. OAuth keys and secrets

### User interface

The User Interface (UI) is expected to be highly customized by each implementation, so that it is embedded in the service providers' look-and-feel.

## Reference implementation

We provide a [reference implementation](https://github.com/google/data-transfer-project/blob/master/extensions/transport/portability-transport-jettyrest/src/main/java/org/datatransferproject/transport/jettyrest/rest/JerseyTransportBinder.java) which runs in a docker image (for example, our [demo image](https://hub.docker.com/r/datatransferproject/demo/)) which allows for deployment to any cloud environment with minimal customization.

## Security and privacy

The security and privacy of user data is a foundational principle of the Data Transfer Project. Because there are multiple parties involved in the data transfer (the user, Hosting Entity, Providers, and Contributors) no one person or entity can fully ensure the security and privacy of the entire system. Instead, responsibility is shared among all the participants. Here are some of responsibilities and leading practices that contribute to the security and privacy of the DTP system.

### Data minimization
When transferring data between Providers, data minimization should be practiced. Practically this means that the receiving Provider should only process and retain the minimum set of data needed to provide their service. The sending Provider should provide all needed information, but no more. Note: DTP won’t delete data from the sending Provider as part of the transfer. However, participating providers should allow users to delete their data after a successful transfer has been verified.

### User notification
The Hosting Entity should configure their Hosting Platform to notify the user that a data transfer is occurring. Ideally, the user of the source and destination account are the same. However, user notification is designed to help protect against situations where that is not the case, and so notifications alerting the user of the transfer request should be sent to both the source account and the destination account. Depending on the sensitivity of the data being transferred, the Hosting Entity should consider delaying the start of the transfer so that the user has the opportunity to cancel the transfer after receiving the notification.

### Rate limiting

Hosting Entities, as well as Providers, should consider rate limiting the number and frequency of transfers for a given user. This approach can help limit the impact of an account compromise. The trade off between ease of use and security with this method means there is not a one size fits all answer as to what rate limit should be set. Instead, Providers and Hosting Entities should evaluate the sensitivity of the data, as well as known and possible attacks, when determining the appropriate rate limiting. 

### Token revocation
When a transfer is completed, DTP will attempt to revoke the authorization tokens used for the transfer. Providers should ensure their API supports token revocation. This is a Defense in Depth approach to ensure that if a token is leaked, its effectiveness will be limited to the duration of the transfer.
Minimal Scopes for Auth Tokens
Providers should offer granular scopes for their authentication tokens. This provides two benefits: first, providing transparency into exactly what data will be moved; second, as a defense in depth mechanism so that if tokens are somehow leaked they have the minimal possible privilege. At a minimum there should be read only versions of all the scopes so no write/modify/delete access is granted on the sending Provider.

### Data retention
DTP only stores data for the duration of the transfer job. Also, all data passing through the system is encrypted both at rest and in transit. Specifically, all data stored at rest is encrypted with a per-user session key that is created and stored in memory of the ephemeral virtual machine that is used to process a single user’s job. The Hosting Entity and Provider are responsible for ensuring that any stored aggregated statistics maintain user privacy.

### Abuse
Providers and the Hosting Entity (if separate from the Provider) should have strong abuse protections built into their APIs. Due to the fact that DTP retains no user data beyond the life of a single transfer, and that there might be multiple Hosting Entities utilized in an attack, the Providers have the best tools to be able to detect and respond to abusive behavior. Providers should carefully control which Hosting Entities are able to obtain API keys. Providers are also encouraged to have strong security around granting auth tokens. Examples of this include requiring a reauthentication or asking security challenge questions before granting access.

## API key management

To access and use an API it is usually required to obtain an API key from the provider first.

Each hosted instance using a service will be required to obtain an API key for transferring data to/from that service.  This ensures that each company is independently responsible for the terms that accompany the API access.

It also ensures that each service provider has the final say in which DTP hosting entities may use their APIs for data transfer. In practice, this means that services will be able to approve or deny companies' requests to offer data transfer with them.

## Reciprocity

A healthy data portability ecosystem requires providers that allow equivalent import and export functionality. Providers that import data but don’t allow a similar level of export pose a risk to users by trapping their data into a service. There are several possible ways to promote reciprocity in the DTP ecosystem. We have identified several methods, which we list below, and will work with Partners to further explore these and other options.

### In source code
Contributions into the main Source Code Repository must contain an importer coupled with each exporter. This is to ensure at least an attempt at reciprocity.

### Transparency
Each hosting provider should provide aggregated statistics about problems users encountered when importing and exporting data to service providers. This aims to ensure that providers are maintain their exporting functionality to a similar level of reliability as their importing functionality.

### Automated fidelity test
Hosting Providers can establish testing accounts with various service providers and do periodic imports and exports of data to each service to ensure that data is exported with the appropriate fidelity. This information can again be provided in a transparent way to users at import time to ensure users can make an informed decision when choosing which providers to entrust their data with. 

### Data portability provider pledge
Providers can work together to create a data portability pledge that requires them to follow best practices on portability. Hosting platforms can seek to support providers that commit to the pledge, user interfaces can display providers that commit to the pledge to users, and reports can be published on the state of the ecosystem with regards to reciprocity.


## V2 Features

The goal of V1 is to create a healthy ecosystem with multiple participants to prove the viability of the model.  However there are clear points of extension that are possible:

-  Selective migrations based on metadata (e.g. tag or date ranged migrations)
-  Enterprise level features
-  Scheduled/recurring migrations

## Glossary of Terms

### Adapters 
Adapters exist outside of a Provider’s core infrastructure and provide the translation between a Provider’s core infrastructure and the DTP environment. 

- __Data Adapters__ translate a Provider’s APIs into Data Models, and vice versa
- __Authentication Adapters__ translate from a Provider’s Authentication to the DTP Authentication

### Data Model 
Data Model is used to describe the file type and associated metadata to describe elements in a Vertical. For example, in the Photos Vertical, a Data Model would include a file type (.jpg, .tiff) and the associated metadata needed to fully describe the .jpg as a photo, such as title, description and album, if applicable. 

### DTP
Data Transfer Project

### Hosting Entity
A Hosting Entity is the entity that runs a Hosting Platform of DTP. In most cases it will be the Provider sending or receiving the data, but could be a trusted third party that wants to enable data transfer among a specific group of organizations.

### Hosting Platform
A Hosting Platform is the technical environment where a DTP instance can be hosted. This can be a cloud environment, enterprise infrastructure, or local. As of March 2018, the supported cloud hosting platforms include Google Cloud Platform and Microsoft Azure.

### Individual
Individuals are any person who interacts with a Provider. Individuals are interested in being able to manage and move the data they have stored in a Provider’s infrastructure.

### Contributors 
Contributors are official members of the Data Transfer Project. They are most likely Providers, but may also be organizations who are interested in enabling data transfer among their members. Contributors contribute in many ways to the Data Transfer Project Open Source project, including contributing code, tools, advice and insights.

### Provider
Providers are any company or entity that holds user data. Providers may or may not be contributors.

### Task Management Library
The Task Management Library is a set of system components designed to coordinate and manage tasks that export and import data including retrieving data, pagination when applicable, handling exceptions utilizing retry strategies, re-creation of folder/container structures, and importing of data.

### Vertical
Verticals represent a collection of Data Models that make up a generic category of data. Some Verticals initially supported in DTP include Photos, Calendar, Tasks, and so on.

#### DTP is early-stage open source code that is built and maintained entirely by DTP community members.
