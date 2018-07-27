import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {Observable} from 'rxjs/Rx';
import {ProgressService} from "../progress";
import {TransferService} from "./transfer.service";
import {StartTransferJob} from "../types";
import {transportError} from "../transport";

/**
 * Initiates a transfer operation.
 */
@Component({
    templateUrl: "./initiate-transfer.component.html"
})
export class InitiateTransferComponent implements OnInit {
    private dataType: string;
    private exportService: string;
    private importService: string;

    initiated = false;

    constructor(private transferService: TransferService, private progressService: ProgressService, private router: Router) {
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
        let pollForWorkerKey = Observable.interval(1500)
          .switchMap(() => this.transferService.getReservedWorker({id: this.progressService.transferId()}))
          .take(20)
          .subscribe(
            reservedWorker => {
                console.log("polling for worker");
                if (reservedWorker.publicKey) {
                  console.log("got worker with key: " + reservedWorker.publicKey);
                  pollForWorkerKey.unsubscribe();
                  this.progressService.workerReserved(reservedWorker.publicKey);
                  this.startTransferJob();
                  // TODO encrypt creds with worker public key
                }
            }, transportError);
    }

    startTransferJob() {
      let start: StartTransferJob = {
            id: this.progressService.transferId(),
            exportAuthData: this.progressService.exportAuthData(),
            importAuthData: this.progressService.importAuthData()
        };
        this.progressService.initiated();
        this.initiated = true;
        this.transferService.startTransferJob(start).subscribe(transferJob => {
      }, transportError);
    }

    reset() {
        this.progressService.reset();
        this.router.navigate([""]);
    }

}
