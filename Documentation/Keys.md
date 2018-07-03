# API Keys
To use the Data Transfer Project each instance needs to get its own set of API keys
for each service they want to interact with. This has many reasons and benefits:

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
