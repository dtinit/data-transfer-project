import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {ProgressService} from "../progress";
import {environment} from "../../environments/environment";

/**
 * Displays the initial start step.
 */
@Component({
    templateUrl: "./start.component.html"
})
export class StartComponent implements OnInit {

    constructor(private progressService: ProgressService, private router: Router) {
    }

    ngOnInit() {
        if ("cleartext" === environment.encryptionScheme) {
            console.warn("Client configured to pass authentication credentials as clear text. This scheme should be used for demo purposes only.");
        }
    }

    next() {
        this.progressService.begin();
        this.router.navigate(["data"]);
    }

}
