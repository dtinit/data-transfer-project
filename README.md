# World Takeout

## Overview
The goal of World Takeout (name is a work in progress) is to make it easy to transfer data 
from one service provider (e.g. Google) to another (e.g. Microsoft).

This document focuses on the technical approaches and aspects of the project.
For a higher level overview of the goals and principals please see:
(coming soon).

You can think of World Takeout as 
 * Adapters/Model
 * Workflow Engine
 * Auth Plugins
 
### Current State
This is very much in it infancy.  Currently we are working on building out
a base set of adapters and models.  The goal isn't to achieve 100% fidelity
immediately, but instead of flush out the idea and build a MVP.

The MVP we are shoot for allows the transfer of data in 3+ data types,
with each data type have 3+ service providers.  The data transfer will
concentrate on getting most and not all of the data from one service
to the other.

The workflow engine is just a for loop that runs locally, and it will
probably stay that way for a considerable period of time.

## Adapters

One of the problems with the naive approach to this is that it scales at O(N^2),
for each pair of partners you need to have a flow to migrate data.

World Takeout approach is to reduce the problem to O(N).

To do this we need to think in terms of data types (e.g. photos, mail,
contacts, documents, playlists).  Each service provider may have data
in one or more data types.

For each data type we need to define one or more models for the data.
A model is a representation of the data, ideally it is based on a
existing standard (either formal or ad hoc).  However we recognize
that might not always be possible.

Once one or more models have been defined for a data type an adapter
can be written from a service providers proprietary API representation
of the data into one or more of the common models.

We recognize that it may not always be possible (or preferable)
to have just one model for a data type (think about photos, there
are lots of file formats each with pros and cons).  However
for this to work the number of models has to be much closer
to 1 than to the number of providers using that data type.

## Workflow Engine

Once you have the adapters and models it is theoretically possible to
move data between any two participating providers. The workflow
engine is the part that actually does the moving.  It handles things
like rate limiting and retries.

*None of this is implemented yet*

Goals:
 * be written in a platform independent way (i.e. it is able to 
   run in GCP, AWS or Azure)
 * Be secure
 
## Auth Plugins

World Takeout aims to be auth agnostic.  Service providers should
be able to use any authorization scheme they want.  OAuth is likely
to be a popular choice, however it doesn't need to be the only choice.
Also individual implementations of OAuth can vary.  So each service
provider needs to implement an authorization plug in that lets user
authorize access to their data in whatever way the service provider
chooses.

## Development

### Building/Running
 * Add a secrets.csv file to src/main/resources, this should contain
   all your API keys, see secrets_template.csv for an example.
 * "mvn compile" will build the project.
 * "mvn exec:java" will run the project.

### Contributing
TBD

### Running
TBD
