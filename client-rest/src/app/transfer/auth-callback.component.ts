import {Component, OnInit} from "@angular/core";
import {TransferService} from "./transfer.service";
import {ProgressService, Step} from "../progress";
import {ActivatedRoute, Router} from "@angular/router";
import {transportError} from "../transport";

/**
 * Receives callbacks from external OAuth export and import services. Updates the application state with corresponding OAuth tokens.
 *
 * Authentication is handled in several steps:
 *
 * 1. After receiving the callback and token (code) from the export OAuth service, this component requests the API server to generate export auth data, which it stores as part
 *    of application state.
 *
 * 2. Import auth configuration is generated and the browser is redirected to the import OAuth service.
 *
 * 3. After the user has authenticated, the import OAuth service will redirect the browser back to this compenent, passing the import auth token (code).
 *
 * 4. This component requests the API server to generate import auth data, which it stores as part of application state.
 *
 * 5. The transfer process is continued.
 */
@Component({
    template: "<div></div>"
})
export class AuthCallbackComponent implements OnInit {

    constructor(private progressService: ProgressService, private transferService: TransferService, private route: ActivatedRoute, private router: Router) {
    }

    ngOnInit() {
        let code = this.route.snapshot.queryParams["code"];
        let transferId = this.progressService.transferId();

        if (Step.AUTHENTICATE_EXPORT === this.progressService.currentStep()) {
            // export auth step: generate the export auth data from the export token
            this.transferService.generateAuthData({id: transferId, authToken: code, mode: "EXPORT"}).subscribe((data => {
                this.progressService.authExportComplete(data.authData);

                // retrieve the configuration for the import OAuth service
                this.transferService.prepareImport(transferId).subscribe(transfer => {
                    // redirect to the import OAuth service. When authentication is complete, the browser will be redirected back to this component
                    window.location.href = transfer.link;
                }, transportError);
            }));
        } else {
            // import auth step: received the import auth callback, generate the import auth data from the import token and continue the transfer process
            this.transferService.generateAuthData({id: transferId, authToken: code, mode: "IMPORT"}).subscribe(data => {
                this.progressService.authImportComplete(data.authData);
                this.router.navigate(["initiate"]);
            }, transportError);
        }
    }
}