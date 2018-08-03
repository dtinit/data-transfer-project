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
        // Poll until a worker public key is available, indicating a worker has been assigned for our transfer job.
        // Once we have this key, we'll use it to encrypt the user's export and import auth credentials and start the
        // transfer job.
        let pollAttempt = 0;
        let maxPollAttempts = 20;
        let pollForWorkerKey = Observable.interval(1500)
          .switchMap(() => this.transferService.getReservedWorker({id: this.progressService.transferId()}))
          .take(maxPollAttempts)
          .subscribe(
            reservedWorker => {
                console.log("polling for assigned transfer worker");
                pollAttempt++;
                if (reservedWorker.publicKey) {
                  // TODO: Remove debug statement in production
                  console.log("got transfer worker with public key: " + reservedWorker.publicKey);
                  pollForWorkerKey.unsubscribe();
                  this.progressService.workerReserved(reservedWorker.publicKey);
                  this.startTransferJob();
                  // TODO: encrypt creds with worker public key
                } else if (pollAttempt == maxPollAttempts) {
                  alert(`Timed out getting a worker for this data transfer`);
                }
            }, transportError);
    }

    startTransferJob() {
      let start: StartTransferJob = {
            id: this.progressService.transferId(),
            exportAuthData: this.progressService.exportAuthData(),
            importAuthData: this.progressService.importAuthData(),
            // TODO: remove flag once client encrypts creds
            authDataEncrypted: false
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
