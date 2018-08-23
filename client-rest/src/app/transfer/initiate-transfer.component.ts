import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {Observable} from 'rxjs/Rx';
import {ProgressService} from "../progress";
import {TransferService} from "./transfer.service";
import {StartTransferJob} from "../types";
import {transportError} from "../transport";
import {Jose, JoseJWE} from "jose-jwe-jws";
import {environment} from "../../environments/environment";


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
                        pollForWorkerKey.unsubscribe();
                        this.progressService.workerReserved(reservedWorker.publicKey);
                        if ("cleartext" === environment.encryptionScheme) {
                            this.clearTextAndStartTransfer();
                        } else {
                            this.encryptAndStartTransfer();
                        }
                    } else if (pollAttempt == maxPollAttempts) {
                        alert(`Timed out getting a worker for this data transfer`);
                    }
                }, transportError);
    }

    clearTextAndStartTransfer() {
        // send auth data as cleartext
        let authData = JSON.stringify({exportAuthData: this.progressService.exportAuthData(), importAuthData: this.progressService.importAuthData()});
        this.startTransfer(authData);
    }

    encryptAndStartTransfer() {
        // encrypt the export/import auth data pair as a JWE using the public key associated with the transfer job
        // .cf https://tools.ietf.org/html/rfc7516#page-32
        let cryptographer = new Jose.WebCryptographer();
        (<any>cryptographer).setContentEncryptionAlgorithm("A128CBC-HS256"); // workaround missing method definition in Typescript type definition for WebCryptographer
        cryptographer.setKeyEncryptionAlgorithm("RSA-OAEP");
        let serializedKey = JSON.parse(this.progressService.workerPublicKey());
        Jose.Utils.importRsaPublicKey(serializedKey, "RSA-OAEP").then(rsaKey => {
            let encrypter = new JoseJWE.Encrypter(cryptographer, rsaKey);
            // encrypt the auth data
            let authData = JSON.stringify({exportAuthData: this.progressService.exportAuthData(), importAuthData: this.progressService.importAuthData()});
            encrypter.encrypt(authData).then(encryptedData => {
                this.startTransfer(encryptedData);
            });
        });
    }

    startTransfer(encryptedData: string) {
        let transferId = this.progressService.transferId();
        let start: StartTransferJob = {
            id: transferId,
            encryptedAuthData: encryptedData
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
