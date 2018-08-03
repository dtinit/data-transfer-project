/**
 * Data required to start a transfer job.
 */
export interface StartTransferJob {
    id: string;
    exportAuthData: string;
    importAuthData: string;
    authDataEncrypted: boolean;
}
