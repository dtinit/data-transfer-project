// This file can be replaced during build by using the `fileReplacements` array.
// `ng build ---prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
    production: false,
    encryptionScheme: "jwe", // supported values are "jwe" and "cleartext" which must correspond to the schemes supported by the server
    apiBaseUrl: ((typeof document !== "undefined") && document.baseURI.replace(/\/+$/, "")) || 'https://localhost:3000'
};
