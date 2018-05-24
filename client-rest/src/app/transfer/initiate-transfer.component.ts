import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {EventService} from "../event";
import {ProgressService} from "../progress";
import {TransferService} from "./transfer.service";
import {StartTransfer} from "../types";

/**
 * Initiates a transfer operation. Previosuly generated authentication data is posted to the API server.
 */
@Component({
    templateUrl: "./initiate-transfer.component.html"
})
export class InitiateTransferComponent implements OnInit {
    private dataType: string;
    private exportService: string;
    private importService: string;

    initiated = false;

    constructor(private transferService: TransferService, private progressService: ProgressService, private eventService: EventService, private router: Router) {
    }

    ngOnInit() {
        this.dataType = this.progressService.dataType();
        this.exportService = this.progressService.exportService();
        this.importService = this.progressService.importService();
        if (this.dataType == null || this.exportService == null || this.importService == null) {
            this.progressService.reset();
            this.router.navigate([""]);
            return;
        }
    }

    initiate() {
        let start: StartTransfer = {
            id: this.progressService.transferId(),
            exportAuthData: this.progressService.exportAuthData(),
            importAuthData: this.progressService.importAuthData()
        };
        this.progressService.initiated();
        this.initiated = true;
        this.transferService.initiateTransfer(start).subscribe(transfer => {
        }, (error) => {
            console.error(error);
            alert(`Sorry, something is not right.\n\nCode: ${error.status}\nMessage: ${error.message}`);
        });
    }

    reset() {
        this.progressService.reset();
        this.router.navigate([""]);
    }

}
