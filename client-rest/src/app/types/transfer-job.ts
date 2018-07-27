/**
* A transfer job.
*/
export class TransferJob {
    /**
    * The unique transfer id, url-encoded.
    */
    id: string;

    /**
    * The url to redirect to for export auth.
    */
    exportUrl: string;

    /**
    * The url to redirect to for import auth.
    */
    importUrl: string;
}
