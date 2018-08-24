import {Component, OnInit} from "@angular/core";
import {ActivatedRoute, Router} from "@angular/router";
import {ProgressService} from "../progress";
import {environment} from "../../environments/environment";

/**
 * Displays the initial start step.
 *
 * Also serves as an entry point to redirect to auth component if we are in the authorization flow.
 */
@Component({
    templateUrl: "./start.component.html"
})
export class StartComponent implements OnInit {

    constructor(private progressService: ProgressService, private router: Router, private route: ActivatedRoute) {
    }

    ngOnInit() {
        this.progressService.begin();
        if (this.progressService.authorizing()) {
          let token = this.getToken();
          this.progressService.tokenReceived(token);
          this.router.navigate(["auth"]);
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
      
        if ("cleartext" === environment.encryptionScheme) {
            console.warn("Client configured to pass authentication credentials as clear text. This scheme should be used for demo purposes only.");
        }
        return this.route.snapshot.queryParams["frob"];  // Legacy auth token
    }

    next() {
        this.router.navigate(["data"]);
    }
}
