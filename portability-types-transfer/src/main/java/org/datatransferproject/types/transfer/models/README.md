# Data Models

Data Models are a common format for transferring data between two adapters.

For any vertical (e.g. data type) there will hopefully be between 1 and a small handful
of data models.

## Philosophy

Data Models are meant to be tools to transfer data internally in DTP. They are **not meant to be 
the one true representation** of this type of data outside of DTP.

If there are existing standards for a type of data then that schema/standard should be used instead
of inventing a new schema.  However if there isn't a well adopted standard than providers are free
to come up with their own schema.

Ideally there is just one data model per vertical. That ensures that there is not fragmentation
and every service can transfer data to every other service. However, we want DTP to spur
innovation and not stifle it.  A common pattern we have seen is that new innovative services
offer new innovative features that can't encoded by existing standards. So we want new
services to be able to use DTP even if they have features that aren't yet standardized.

##  New Data Models
New data models should be added as needed.  If you are the first service
provider in a vertical it is up to you to pick a data model.

If you are a new provider in an existing vertical and don't like the current data model you have
two options:

 1) Utilize the existing data model.  We know it might not be your first choice, but it will
    be a lot less work and make the ecosystem better.

 1) Define a new data model. This allows you the flexibility to define the perfect data model.
    However it carries a significant tax:
 
    - If you do this, we **highly recommend** that you only import data from data
      models that you also export data to. So if you want access to other provider's data, or want
      your users to be able to export to other providers it is up to you to write adapters for
      the other services to your new data model.
      
 We believe by putting the onus of updating adapters on the creator of the new data model we allow
 the flexibility of not having DTP locked into a bad initial schema choice, but ensure that the
 cost of the churn associated with changing or adding a data model is born by the initiator of the
 change.

##  Advice on Picking a Schema
Many smart people have put a lot of thought into the right schemas for data types.  We want to
utilize that work in DTP and not reinvent the wheel.

<!--TODO: Add more examples of existing schema repositories. -->
So for new schemas we highly recommend that you check places like [schema.org](https://schema.org)
for existing schemas.

## Changing a schema
For contributors wanting to change an existing schema, please ask your self the following question
before you do a lot of work, the maintainers will ask this question:

"How are DTP users better off by making this change?"

We are very much trying to avoid bike-shedding and centithreads over the schemas we use.
All schemas have flaws, we understand our schemas aren't perfect, they aren't meant to be. 
They are just meant to service the need to transfer data in DTP between adapters.

If there is a concrete use case in DTP that would be enabled by changing a schema than we should
change it.

If you are willing to update everyone's adapter and it doesn't result in any functionality or
fidelity loss, then you can change a schema.

If your schema is theoretically better than the existing schema, we believe you, but lets wait to
change the schema until it is causing problems.

Just like all changes, any schema changes must keep all tests passing.
