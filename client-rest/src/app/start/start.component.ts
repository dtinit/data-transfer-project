import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {ProgressService} from "../progress";

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
    }

    next() {
        this.progressService.begin();
        this.router.navigate(["data"]);
    }

}
