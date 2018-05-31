import {Component} from "@angular/core";
import {ProgressService, Step} from "./progress.service";
import {ActivatedRoute} from "@angular/router";

/**
 * Displays the progress bar based on the application state.
 */
@Component({
    selector: "progress-component",
    templateUrl: "./progress.component.html"
})
export class ProgressComponent {

    constructor(protected progressService: ProgressService, private route: ActivatedRoute) {
    }

    beginCurrent(): boolean {
        return this.route.snapshot.url.length === 0; // default url
    }

    beginComplete(): boolean {
        return this.progressService.currentStep() > Step.BEGIN;
    }

    dataCurrent(): boolean {
        return this.route.snapshot.url.length === 1 && this.route.snapshot.url[0].path === "data";
    }

    dataComplete(): boolean {
        return this.progressService.currentStep() > Step.DATA;
    }

    createCurrent(): boolean {
        return this.route.snapshot.url.length === 1 && this.route.snapshot.url[0].path === "create";
    }

    createComplete(): boolean {
        return this.progressService.currentStep() > Step.CREATE;
    }

    authComplete(): boolean {
        return this.progressService.currentStep() > Step.AUTHENTICATE_IMPORT;
    }

    initiateCurrent(): boolean {
        return this.route.snapshot.url.length === 1 && this.route.snapshot.url[0].path === "initiate";
    }

    initiateComplete(): boolean {
        return this.progressService.currentStep() > Step.INITIATE;
    }


}
