/**
 * Data required to intiate a transfer operation.
 */
export interface StartTransfer {
    id: string;
    exportAuthData: string;
    importAuthData: string;
}