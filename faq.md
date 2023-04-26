---
title: Project Updates
redirect_to: https://dtinit.org/faq
---
<div class="section" markdown="1">
## Q: Are there common standards by which Data Transfer Project partners should abide in performing transfers?
<div class="mustache">
</div>
As described in the white paper, DTP adheres to the following principles:

We believe the following principles around interoperability and portability of data promote user choice and encourage responsible product development, maximizing the benefits to users and mitigating the potential drawbacks.

  * **Build for users**: Data portability tools should be easy to find, intuitive to use, and readily available for users. They should also be open and interoperable with standard industry formats, where applicable, so that users can easily transfer data between services or download it for their own purposes.
  * **Privacy and security**: Providers on each side of the portability transaction should have strong privacy and security measures—such as encryption in transit—to guard against unauthorized access, diversion of data, or other types of fraud. It is important to apply privacy principles such as data minimization and transparency when transferring data between providers. When users initiate a transfer they should be told in a clear and concise manner about the types and scope of data being transferred as well as how the data will be used at the destination service. Users should also be advised about the privacy and security practices of the destination service. These measures will help to educate users about the data being transferred and how the data will be used at the destination service. More details are in the Security & Privacy section below.
  * **Reciprocity**: While portability offers more choice and flexibility for users, it will be important to guard against incentives that are misaligned with user interests. A user’s decision to move data to another service should not result in any loss of transparency or control over that data. Individuals should have assurance that data imported to a provider can likewise be exported again, if they so choose. Ultimately, users should be able to make informed choices about where to store their data. We believe that providing transparency around portability will lead to users preferring providers that are committed to reciprocal data portability, over those that are not.
  * **Focus on user’s data**: Portability efforts should emphasize data and use cases that support the individual user. Focusing on content a user creates, imports, approves for collection, or has control over reduces the friction for users who want to switch among products or services or use their data in novel ways, because the data they export is meaningful to them. Portability should not extend to data that may negatively impact the privacy of other users, or data collected to improve a service, including data generated to improve system performance or train models that may be commercially sensitive or proprietary. This approach encourages companies to continue to support data portability, knowing that their proprietary technologies are not threatened by data portability requirements. For a detailed taxonomy of such data, see ISO/IEC 19944:2017.
  * **Respect Everyone**: We live in a collaborative world: people connect and share on social media, they edit docs together, and they comment on videos, pictures and more. Data portability tools should focus only on providing data that is directly tied to the person requesting the transfer. We think this strikes the right balance between portability, privacy, and benefits of trying a new service.

</div>

<div class="section" markdown="1">
## Q: What kinds of data can be transferred through DTP?
<div class="mustache">
</div>

The terms of each organization’s API determine the data types that can be transferred between providers. This ordinarily includes data stored in a specific users’s account, but may not be limited to that data, depending on the organizations involved.  Additional or substitute functionality outside of the Data Transfer Project would be required for data transfers requiring particularly high integrity (e.g. health records).

</div>

<div class="section" markdown="1">
## Q: Who is responsible for protecting data before, during, and after the transfer takes place?
<div class="mustache">
</div>

Each organization is responsible for securing and protecting the data stored on its platform, regardless of whether it is supporting a transfer out or receiving a transfer from another organization. Generally, this includes established practices in securing access, authorization, and authentication to public APIs or other mechanisms. Additionally, this includes writing and enforcing policies governing access to that information through an API or other mechanism. Those specific terms govern the conditions of transferring data into or out of each provider. Additionally, there are baseline security requirements detailed in the White Paper, such as  encryption in transit, that should always be adhered to.

</div>

<div class="section" markdown="1">
## Q: When data is transferred, do the Partners all get a copy?
<div class="mustache">
</div>

No, when a user initiates a data transfer, their encrypted information flows from one provider directly to another that is chosen by the users. Only the source service, the destination service (and hosting provider, if it is not the source or destination service) have access to the data.  No other DTP partners or 3rd parties have access to a copy of the data as part of the transfer.

</div>

<div class="section" markdown="1">
## Q: Why aren’t there more, smaller companies in the Project?
<div class="mustache">
</div>

DTP is an open source project centered around the idea that less-resourced companies can use and build on the common models and codebase developed by the community of contributors. All companies are welcome to participate. Although the DTP reduces the technical burdens of service-to-service transfers, development work is required of each participating organization. Deciding to participate in the project may require shifting limited resources from other  priorities. We are continuing to make integrating with DTP easier. Our goal is to help companies of all sizes realize the value of providing users more control over their data.

If you are interested in joining the project, please visit the [community]({{site.baseurl}}/community) page to learn how.

</div>
