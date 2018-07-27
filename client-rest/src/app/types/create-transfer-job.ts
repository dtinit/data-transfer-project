/**
* Data required to create a transfer job.
*/
export interface CreateTransferJob {
    /**
    * Source (export) service.
    */
    source: string;

    /**
    * Target (import) service.
    */
    destination: string;

    /**
    * The data type to transfer.
    */
    dataType: string;

    /**
    * The base URL for auth callbacks.
    */
    baseCallbackUrl: string;
}
