# Google Cloud Data overview

Data is stored in [Google Cloud Datastore](https://cloud.google.com/datastore/).

## JobDataCache

This data is stored hierarchically.
 
 At the top level is a node for the job (Kind: job, id: job id).
 
 Below that is one or more service nodes (Kind: service, id: service name).
 
 Below each service are all the user specified keys (Kind: JobData, id: user supplied key).
 
 The job will have a started attribute to allow for cleaning up in the case of failure.
 
 ## Authorization
 
 This requires two keys to be present in the secrets file:
 
   * GOOGLE_PROJECT_ID
     * The project ID for the Google Cloud project (must have billing
       enabled).
   * GOOGLE_DATASTORE_CREDS_FILE
     * The name of the file containing the JSON creds for this user.
       A file of this name should also be placed in the resources
       folder.