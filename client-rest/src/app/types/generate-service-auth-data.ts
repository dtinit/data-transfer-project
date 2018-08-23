/**
 * Data required to generate auth data.
 */
export interface GenerateServiceAuthData {
    id: string;
    authToken: string;
    mode: "EXPORT" | "IMPORT";
    callbackUrl: string;
}
