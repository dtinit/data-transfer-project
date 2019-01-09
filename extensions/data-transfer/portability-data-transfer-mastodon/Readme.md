# Mastodon
This folder contains the extension implementation for the
[Mastodon](https://joinmastodon.org/) API.


## Data Supported

 - Social Activity streams (notes < 500 chars only)

## Current State

 - Proof of concept
 
 Currently the importer and exporter work, and can be exercised via the ManualTest.java
 file.  Just enter your access token from your Mastodon instance UI.
 
 TODO: implement the auth flow for Mastodon, allowing a user to select an instance
 and then automatically registering an app and going through the OAuth flow.
 
 **Known Issues:**
 
 - For data import/export right now this only handles the raw text and not images
 - Imported data is marked as private, ideally a user could mark things as public latter
   but that doesn't appear to be supported buy the Mastodon UI

## Keys & Auth

See above.

## Maintained By

The Google extension was created and maintained by the
[DTP maintainers](mailto:portability-maintainers@googlegroups.com)
(this includes developers from Google).
