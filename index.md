---
title: Data Transfer Project
tagline:  a platform for transferring data
description: Data Transfer Project is a collaboration of organizations committed to building a common framework with open-source code that can connect any two online service providers, enabling a seamless, direct transfer of data between the two platforms.
---

<div class="section" markdown="1">
## About us
<div class="mustache">
</div>

The Data Transfer Project was launched in 2018 to create an open-source, service-to-service data portability platform so that all individuals across the web could easily move their data between online service providers whenever they want.

The contributors to the Data Transfer Project believe portability and interoperability are central to innovation. Making it easier for individuals to choose among services facilitates competition, empowers individuals to try new services and enables them to choose the offering that best suits their needs.

Current contributors include:

<div class="contributors-container">
	<img class="logo-image" src="./images/logo-apple.png">
	<img class="logo-image" src="./images/logo-facebook.png">
	<img class="logo-image" src="./images/logo-google.png">
	<img class="logo-image" src="./images/logo-microsoft.png">
	<img class="logo-image" src="./images/logo-twitter.png">
</div>


<div class="section" markdown="1">
<div class="section-text" markdown="1">
## What is the Data Transfer Project
<div class="mustache">
</div>
Data Transfer Project (DTP) is a collaboration of organizations committed to building a common framework with open-source code that can connect any two online service providers, enabling a seamless, direct, user initiated portability of data between the two platforms.

[Learn More](./what-is-dtp.md){: .learn-more}    
</div>
<div class="section-image-container"><img class="section-image" src="./images/AI_Blue_FileSharing.png"></div>
</div>

<div class="section" markdown="1">
## How does it work
<div class="mustache">
</div>

The Data Transfer Project uses services' existing APIs and authorization mechanisms to access data. It then uses service specific adapters to transfer that data into a common format, and then back into the new service’s API.
</div>

[Learn More](./how-does-dtp-work.md){: .learn-more}  
</div>

<div class="section" markdown="1">
## What are some use cases
<div class="mustache">
</div>

There are many use cases for users porting data directly between services, some we know about today, and some we have yet to discover. A couple of examples of ones we know users want today are:

  * Trying out a new service
  * Leaving a service
  * Backing up your data

[Learn More](./use-cases.md){: .learn-more}  
</div>

<div class="section" markdown="1">
<div class="section-text" markdown="1">
## Why do we need DTP
<div class="mustache">
</div>
Users should be in control of their data on the web, part of this is the ability to move their data. Currently users can download a copy of their data from most services, but that is only half the battle in terms of moving their data. DTP aims make move data between providers significantly easier for users.

[Learn More](./why-dtp.md){: .learn-more}  
</div>
<div class="section-image-container"><img class="section-image" src="./images/AI_Blue_QA.png"></div>
</div>  

<div class="section" markdown="1">
## Technical Overview
<div class="mustache">
</div>

To get a more in-depth understanding of the project, its fundamentals and the details involved, please download [“Data Transfer Project Overview and Fundamentals”](./dtp-overview.pdf). You can also use the button at the top of the page.

<a href="./dtp-overview.pdf" class="download-link" ><img class="download-image" src="./images/download.png"></a>
</div>

<div class="section" markdown="1">
## How to try it out
<div class="mustache">
</div>

DTP is still in development and is not quite ready for everyone to use yet. However if you want to give it a shot we have a couple methods you can try out:

Note: For both of these you need to get your own API keys from the services you want to port data between; please see instructions for getting keys.

  * [Via Docker](https://github.com/google/data-transfer-project/blob/master/Documentation/RunningLocally.md)
  * [Via Code](https://github.com/google/data-transfer-project/blob/master/Documentation/Developer.md)
  * Sites powered by DTP (coming soon)
</div>

<div class="section" markdown="1">
## How to participate
<div class="mustache">
</div>

We welcome everyone to participate, the more expertise and viewpoints we have contributing to the project the more successful it will be.

  * If you are a company that wants to add an integration to allow your users to easy import and export data: [Learn More](https://github.com/google/data-transfer-project/blob/master/Documentation/Integration.md) 
  
  * If you are an individual that wants to contribute technically please see our contributions guide: [Learn More](https://github.com/google/data-transfer-project/blob/master/Documentation/Developer.md) 
  
  * If you want to be involved in non-technical aspects of the project you can join the discussion at dtp-discuss or email the maintainers at portability-maintainers@googlegroups.com  
</div>

<div class="section" markdown="1">
<div class="section-text" markdown="1">
## Current State
<div class="mustache">
</div>
DTP is still in very active development, and while we have code that works for a variety of use cases, we are continually making improvements that might cause things to break occasionally. So as you are trying things please use it with caution and expect some hiccups. Both our bug list, as well as documentation in each provider’s directory are good places to look for known issues, or report problems you encounter.

</div>

<div class="section-image-container"><img class="section-image" src="./images/AI_Blue_User data.png"></div>
</div>

<div class="section" markdown="1">
## Project Updates
<div class="mustache">
</div>
We're excited about the progress we've made since we got this effort off the ground in July 2018. 
  * Apple has officially joined the Project.
  * 18 contributors from a combination of partners and the open source community have inserted more than 42,000 lines of code and changed more than 1,500 files.
  * We've added framework features such as Cloud logging and monitoring to enable production use of the Data Transfer Project at companies developing new features.
  * We also updated integrations for new APIs from Google Photos and Smugmug that will enable users to move their photos between these services.
  * We have added new integrations for Deezer, Mastodon, and Solid.

The engineering work we’ve done through the Project has highlighted the importance of working with stakeholders to develop guidelines around portability. If you'd like to follow the latest developments with the Project, [sign up](https://groups.google.com/forum/#!forum/dtp-discuss) for dtp-discuss@googlegroup.com.
</div>
