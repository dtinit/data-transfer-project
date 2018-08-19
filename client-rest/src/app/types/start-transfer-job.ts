/**
 * Data required to start a transfer job.
 */
export interface StartTransferJob {
    id: string;

    /**
     * A JWE containing the auth data pair in the form {exportAuthData: AuthData, importAuthData: AuthData}
     */
    encryptedAuthData?: string;
}
