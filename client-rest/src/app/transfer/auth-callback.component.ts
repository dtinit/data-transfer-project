import {Component, OnInit} from "@angular/core";
import {TransferService} from "./transfer.service";
import {ProgressService, Step} from "../progress";
import {ActivatedRoute, Router} from "@angular/router";
import {transportError} from "../transport";
import {environment} from "../../environments/environment";

/**
 * Receives callbacks from external OAuth export and import services. Updates the application state with corresponding OAuth tokens.
 * This component supports both OAuth 1 and OAuth 2 tokens as well as legacy (frob) tokens.
 *
 * Authentication is handled in several steps:
 *
 * After receiving the callback and token (code) from the export OAuth service, this component stores the raw auth
 * data/tokens locally, and either:
 * - 1st callback: redirects to the import auth URL, if we are handling the (first) callback from export auth
 * - 2nd callback: calls ReserveWorker in the API server, if we are handling the (second) callback from import auth
 *
 * From there, the transfer process is continued server-side.
 */
@Component({
    template: "<div></div>"
})
export class AuthCallbackComponent implements OnInit {

    constructor(private progressService: ProgressService, private transferService: TransferService, private route: ActivatedRoute, private router: Router) {
    }

    ngOnInit() {
        let token = this.getToken();
        let transferId = this.progressService.transferId();
        let importUrl = this.progressService.importUrl();

        if (Step.AUTHENTICATE_EXPORT === this.progressService.currentStep()) {
            // export auth step: received the export auth callback, generate the export auth data from the export token
            // and redirect to the import url. Use SSL as the token is sent cleartext.
            this.transferService.generateAuthData({
              id: transferId,
              authToken: token,
              mode: "EXPORT",
              callbackUrl: `${environment.apiBaseUrl}/callback/${this.progressService.exportService().toLowerCase()}`
            }).subscribe(
            (data => {
                this.progressService.authExportComplete(data.authData);

                // Redirect to the import OAuth service. When authentication is complete, the browser will be redirected
                // back to this component
                window.location.href = this.progressService.importUrl();
            }));
        } else {
            // import auth step: received the import auth callback, generate the import auth data from the import token
            // and continue the transfer process. Use SSL as the token is sent cleartext.
            this.transferService.generateAuthData({
              id: transferId,
              authToken: token,
              mode: "IMPORT",
              callbackUrl: `${environment.apiBaseUrl}/callback/${this.progressService.importService().toLowerCase()}`
            }).subscribe(
            data => {
                this.progressService.authImportComplete(data.authData);

                this.transferService.reserveWorker({id: transferId}).subscribe(
                unused => { // TODO: make this return void?
                    this.router.navigate(["initiate"]);
                }, transportError);
            }, transportError);
        }
    }

    /**
     * Returns the OAuth 2, OAuth 1 or FOB token depending on the authentication protocol being used.
     */
    private getToken() {
        let code = this.route.snapshot.queryParams["code"];  // OAuth 2 token, if using OAuth 2
        let oAuthVerifier = this.route.snapshot.queryParams["oauth_verifier"];  // OAuth 1 token, if using OAuth 1

        if (code != null) {
            return code;
        } else if (oAuthVerifier != null) {
            return oAuthVerifier;
        }
        return this.route.snapshot.queryParams["frob"];  // Legacy auth token
    }
}
