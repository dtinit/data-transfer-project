## Data Transfer Project Use Cases
<div class="mustache">
</div>

Individuals have many reasons to transfer data, but we want to highlight a few examples that demonstrate the additional value of service-to-service portability.

A user discovers a new photo printing service offering beautiful and innovative photo book formats, but their photos are stored in their social media account. With the Data Transfer Project, they could visit a website or app offered by the photo printing service and initiate a transfer directly from their social media platform to the photo book service.
A user doesn’t agree with the privacy policy of their music service. They want to stop using it immediately, but don’t want to lose the playlists they have created. Using this open-source software, they could use the export functionality of the original Provider to save a copy of their playlists to the cloud. This enables them to import the lists to a new Provider, or multiple Providers, once they decide on a new service.
A large company is getting requests from customers who would like to import data from a legacy Provider that is going out of business. The legacy Provider has limited options for letting customers move their data. The large company writes an Adapter for the legacy Provider’s Application Program Interfaces (APIs) that permits users to transfer data to their service, also benefiting other Providers that handle the same data type.
A user in a low bandwidth area has been working with an architect on drawings and graphics for a new house. At the end of the project, they both want to transfer all the files from a shared storage system to the user’s cloud storage drive. They go to the cloud storage Data Transfer Project User Interface (UI) and move hundreds of large files directly, without straining their bandwidth.
An industry association for supermarkets wants to allow customers to transfer their loyalty card data from one member grocer to another, so they can get coupons based on buying habits between stores. The Association would do this by hosting an industry-specific Host Platform of DTP.
The innovation in each of these examples lies behind the scenes: Data Transfer Project makes it easy for Providers to allow their customers to interact with their data in ways their customers would expect. In most cases, the direct-data transfer experience will be branded and managed by the receiving Provider, and the customer wouldn’t need to see DTP branding or infrastructure at all.

The illustration below demonstrates what an interaction might look like. In this case, the customer wants to join a new Provider (Microsoft) and is requesting their files from their existing Provider (Google):

<img src="./images/dashboard-flow.png" />
