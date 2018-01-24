# Data Portability Project Overview
2017-Oct-31

## Vision

Users should be able to easily transfer their data directly between online service providers.

The project aims to achieve this by providing a common framework and ecosystem to allow service provides to contribute and allow seamless transfer of data into and out of their service.

## Guiding Principles

### User Driven  
Data portability tools should be easy to find, intuitive to use, and readily available to consumers.

### Privacy and Security  
Providers on each side of the portability transaction should have strong privacy and security measures---such as encryption in transit---to guard against unauthorized access, diversion of data, or other types of fraud.

### Reciprocity  
While portability offers more choice and flexibility for users, it will be important to guard against incentives that are misaligned with user interests. A user's decision to move data to another service should not result in any loss of transparency or control over that data. Users will expect that data ported into a provider can likewise be exported again, if they so choose.  

### Focus On Users' Data, Not Company Data  
Data portability is not, and should not be, without reasonable limits. Portability efforts should be limited to data that has utility for a user, e.g. the content a user creates, imports, approves for collection, or has control over.

## Benefits of Portability

### Prevent lock-in  
Users should be able to choose the service that best meets their needs, not be stuck with the first service they used just because it holds all their data and they are afraid of losing it.

### Foster Innovation  
If new products can easily have access to existing user data, it should lower the bar for entry into a market.

### Promote competition  
Services won't be able to rest on past successes if users can easily transfer data to new services. This should hopefully increase competition in the marketplace, both from existing services and from new services.

## Example/Targeted Use Cases

### Switching to a new service  
Users should be able to move completely from one service to another.  For example, if Alice wants to move from Google Drive to Office 365 she should be able to easily move all of her documents over and then delete all of her old data in Google Drive.

### Trying out a new service  
Users should be able to try out new services.  For example, Bob hears about a new email service with an amazing new UI, but it's hard to try it out without significant amounts of real data in it.  Bob should be able to easily make a copy of all his old email in the new service and try it out.  Then he can decide to delete the data in either of providers or keep using them both.

### Moving selected data  
Users should be able to move selected parts of their data to new companies.  For example, Charlie keeps all of her Photos in Google Photos, but really wants to make a photobook service to print a photo book.  She should be able to move just the pictures from the last year over to the photobook service to create the photo book without affecting her Google Drive usage at all.

## Top Level Components

The project is broken out into five main pieces: Data Specifications, Adapters, Task Management, Cloud Interface and UI.

It is important that all ecosystem partners work off of the same Data Specifications and Adapters.  However the system is designed so that the Task Management, Cloud Interface, and UI can all be swapped out as needed.


### Data Specifications

One or more shared interchange formats for the various data types (e.g. tasks, docs, photos, playlists) following commonly used industry formats.

### Adapters  

Vendor-specific plugins that convert from proprietary APIs and formats into one or more common data specifications.

### Task Management  

A system to manage jobs and tasks. It calls the two relevant adapters, and manages paging, rate limiting, and retry logic.

### Cloud Interface  

A platform specific implementation of needed infrastructure services, e.g. storage, docker image execution, etc.

### UI  

The UI layer that talks to the task management layer to manage migration jobs.

## Deployment & Use

All the code for the project lives in a centralized open source repository.  Ideally the common code meets most use cases.  However, other entities are free to fork and extend the project to meet their specific needs.

Each organization interested in porting data runs an instance of the service (optionally with custom UI, Cloud Interface, and Task Management components).

From a cost and security standpoint, the ideal scenario is that the system works as a pull model: if a user is trying to move their data from service A to service B, they would initiate the transfer via service B. However it seems likely that in the case where a user wants to move from LargeCorp to TinyStartup, TinyStartup might not have yet set up a full instance to power the move, and in that case we would suggest that LargeCorp allow the transfer to be started from their service.

### Possible Alternative  
A (not mutually exclusive) option is to have a centralized common instance/UI running that allows transfers between any two vendors in the system.  Pros: easy to brand/market with a central solution, good user experience, easy to enforce reciprocity (see below), easier to manage API keys.  Cons: requires lots of coordination, cost sharing is hard, is a security concern.

## Detailed Component Design

### Data Specifications

Representations of common data attributes for each data type providing for a common set of data attributes that are portable across service providers.

Where standards exist, the model will encapsulate the standard.

Example model for data type "Photo" vertical:

> class PhotosModelWrapper implements DataModel {  
  Collection<PhotoAlbum> albums;  
  Collection<PhotoModel> photos;  
  ContinuationInformation continuationInformation;  
}

> class PhotoAlbum {  
  String id;  
  String name;  
  String description;  
}

> class PhotoModel {  
  String title;  
  String fetchableUrl;  
  String description;  
  String mediaType;  
  String albumId  
}

For a given vertical there doesn't need to be only one data specification, but there should be no more than a handful.

### Adapter Plugin Model

Each adapter needs to provide 3 pieces of functionality: authorization, export, and import.

Authorization is ideally provided by OAuth, in which case a common auth code can be reused, but adapters are free to define arbitrary auth protocols as needed.

The export interface can be implemented to allow each service's adapter code to retrieve data and return it in a common data model format.  It also allows for continuation and pagination of data:   
	<T extends DataModel> export (ExportInformation continuationInformation)
ExportInformation contains an optional Resource and an optional PaginationInformation.  A Resource represents a collection (such as an album or playlist) so that it is clear what collection subsequent data should be added to.  A PaginationInformation contains information about what the next page is - for example, if the job is currently looking at page 2 of a collection, then PaginationInformation should direct to page 3.

The import interface allows for implementations to import or upload the data stored in a common data model format, which may include a collection of data or single data item:   
void importItem(<T extends DataModel> object)

### Task Management

This component handles starting, and managing individual data migrations.  This includes things like persisting state, handling pagination, managing retries and rate limiting (globally, per service, and per user).  One class used for this is the JobDataCache, which can be used for storing information that should be used across multiple instances.

Individual deployments are free to use any task management implementation they choose, however the reference implementation is abstracted away from the cloud layer so that providers can provide the cloud implementation of their choice (Azure, AWS, GCP, etc.) to make this as reusable as possible.


### Cloud Interface

Because all the major tech companies will likely want to run this on their own cloud the entire system is designed to abstract away the specific cloud implementation and only rely on features available in all the major platforms.

Currently the only requirements are:

-  PersistentKeyValueStore - a persistent key value store
-  JobDataCache - Similar to a PersistentKeyValueStore but able to delete all data related to a job at once.
-  Ability to run docker images

### UI

This component is expected to be highly customized by each implementation.  It is likely that each implementation might have multiple UIs, e.g. a portal that allows users to migrate data from all a provider's verticals and also an in product UI that allows users to migrate data for just one vertical.

## Reference Implementation

A reference implementation running as a docker image using open source and general cloud apis which allows for deployment to any cloud environment with minimal customization.

### Architecture

TBD

## Security

Because of the sensitive nature of the data in the system (user credentials and content) security is of the utmost importance.

The security properties of a given instance are going to be largely dependent on the Task Management implementation, UI, and cloud deployment of an individual instance.

Our reference implementation aims to be as secure as possible.  And includes the following features:

-  Ephemeral keys that are never persisted, not accessible to administrators without forensic means.
-  All persisted data is encrypted with an ephemeral key
-  Role and Privilege separation
-  Tight internal and external firewalls
-  Ephemeral VM instances to ensure exploits don't persist from job to job
-  Jobs run on dedicated VMs, so exploits can't cross jobs
-  Dynamic code loading, only code from 2 providers is loaded

By its nature, the system must be able to run third-party adapter code to a variety of systems, thus, all code should be treated as potentially malicious.

Some mitigation steps are code and network isolation for each configured service.

In addition, the reference implementation will provide for security around user credentials and job data. For example, a public/private key pair will exist between the client and worker instance processing the migration of data, isolating the potential leak of information from any single point in the system. User creds and other sensitive data that is retrieved from the authentication flows will be encrypted at rest.

## API Key Management

To access and use an API it is usually required to obtain an API from the provider first.

The plan is that each instance of this service will be required to obtain the API's keys they need to transfer data to the desired service.  This ensures that each company is independently responsible for the terms that accompany the API access.  And that each company has the final say in who they do and do not allow transfer to (they can either not ask for, or not grant an API to a company at their sole discretion).

## Reciprocity

Reciprocity is key to this project.  Allowing import of data is very clearly a priority for companies, but ensuring that their export solutions function just as well is rarely a priority.

In order to ensure that the ecosystem stays healthy it is important to ensure providers' export and import functionalities function equally well.

This can be accomplished in a number of ways, and there are subtle pros and cons to each method:

-  No import-only integrations will be allowed.  For any data specification you want to import data from you must also include export functionality.
-  Error metric-based reporting.  Each instance should publish (method TBD) error rates for import and export success ratios per service.  This allows for transparency of companies practices.
-  Error metric-based enforcement.  Same as above, but instead of just shaming companies a blacklist is published and automatically disabled import into services with broken exports.
-  Round trip based tests to ensure comparable fidelity of import/export with known test accounts.

## V2 Features

The goal of V1 is to create a healthy ecosystem with multiple participants to prove the viability of the model.  However there are clear points of extension that are possible:

-  Selective migrations based on metadata (e.g. tag or date ranged migrations)
-  Enterprise level features
-  Scheduled/recurring migrations

## Definitions

Vertical - One type of data e.g. photos, Docs, or Location history.

## This is not an official Google product
