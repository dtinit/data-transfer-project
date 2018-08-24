/**
 * Data required to start a transfer job.
 */
export interface StartTransferJob {
    id: string;

    /**
     * The auth data pair in the form {exportAuthData: AuthData, importAuthData: AuthData} encrypted according to the scheme in use by the client and server.
     */
    encryptedAuthData: string;
}
