# API Keys
To run the Data Transfer Project, each hosted instance needs its own API keys
for each service it will facilitate transfers with. These keys allow the
DTP instance to interact with these services' APIS.

This has many reasons and benefits:

 - **Isolation** Each instance of DTP having its own API keys means that each
   instance is isloated from the others.  So quota and abuse issues in one
   instance don't affect another instance.

 - **TOS** Aquiring API keys for a service usaully involves agreeing to their
   Terms of Service.  It is important that each Hosting Provider read, understand
   and agree to any applicable TOS for each service.

 - **Security** Sharing keys is bad for security.

But this does mean that to run an instance of DTP you need to do some leg work
to acquire and manage application credentials.  We encourage all providers to
keep up to date instructions about how acquire application credentials in
each provider's directory.

API keys usually are just the OAuth App Key and Secret for your app.  But DTP supports
arbitrary authorization mechanism, and so the exact form the key is dependent on the
authorization technology each providers uses.

 - [Flickr](../extensions/data-transfer/portability-data-transfer-flickr#keys--auth)
 - [Google](../extensions/data-transfer/portability-data-transfer-google#keys--auth)
 - [Instagram](../extensions/data-transfer/portability-data-transfer-instagram#keys--auth)
 - [Microsoft](../extensions/data-transfer/portability-data-transfer-microsoft#keys--auth)
 - [Remember The Milk](../extensions/data-transfer/portability-data-transfer-rememberthemilk#keys--auth)
 - [SmugMug](../extensions/data-transfer/portability-data-transfer-smugmug#keys--auth)
 - [Twitter](../extensions/data-transfer/portability-data-transfer-twitter#keys--auth)


## Deploying Keys

Depending on how you are running DTP you'll deploy secrets differently.

For demo purposes DTP will read Keys from environment variables. When deployed in production
DTP will use the platform's secret management solution via the DTP cloud extension.

You will only need keys for the specific providers you want to transfer data to/from
not all the services DTP supports.

### Running locally via Jar

If you are running DTP locally via the jars directly you'll need to set the API keys
via environment via your OS.
  - Linux: .bashrc
  - Windows System > Control Panel > Advanced system settings > Environment Variables
 
 The format is <provider>_KEY and <provider>_SECRET, see
 [distributions/demo-server/env.secrets.template](../distributions/demo-server/env.secrets.template)
 for an example.

### Running locally via Docker

When running via Docker you will pass in an envionment file via the `-e <file>` flag
see [distributions/demo-server/env.secrets.template](../distributions/demo-server/env.secrets.template)
for a template.

### Running on a cloud provider

When running on a cloud provider your cloud implementation wil override
[AppCredentialStore](../portability-spi-cloud/src/main/java/org/dataportabilityproject/spi/cloud/storage/AppCredentialStore.java)
to read stored credentials from your cloud provider.
