import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ProgressService} from "../progress";
import {TransferService} from "./transfer.service";
import {transportError} from "../transport";
import {environment} from "../../environments/environment";

/**
 * Creates a transfer job request on the API server.
 * TODO: Add form validation
 */
@Component({
    templateUrl: "./create-transfer.component.html"
})
export class CreateTransferComponent implements OnInit {
    servicesForm: FormGroup;

    dataType: string;
    exportServices: Array<string>;
    importServices: Array<string>;

    constructor(private transferService: TransferService, private progressService: ProgressService, private formBuilder: FormBuilder, private router: Router) {
        this.servicesForm = this.formBuilder.group({
            exportService: [undefined, Validators.required],
            importService: [undefined, Validators.required]
        });
    }

    ngOnInit() {
        this.dataType = this.progressService.dataType();
        if (this.dataType == null) {
            this.progressService.reset();
            this.router.navigate([""]);
            return;
        }
        this.servicesForm.get("exportService").setValue(undefined);
        this.servicesForm.get("importService").setValue(undefined);

        this.transferService.getServices(this.dataType).subscribe((services) => {
            this.exportServices = services.exportServices.sort();
            this.importServices = services.importServices.sort();
        }, transportError);
    }


    next() {
        this.progressService.servicesSelected(this.servicesForm.get("exportService").value, this.servicesForm.get("importService").value);
        this.transferService.createTransferJob({
            exportService: this.progressService.exportService(),
            importService: this.progressService.importService(),
            exportCallbackUrl: `${environment.apiBaseUrl}/callback/${this.progressService.exportService().toLowerCase()}`,
            importCallbackUrl: `${environment.apiBaseUrl}/callback/${this.progressService.importService().toLowerCase()}`,
            dataType: this.progressService.dataType(),
            encryptionScheme: environment.encryptionScheme
        }).subscribe(transferJob => {
            // redirect to OAuth service
            this.progressService.createComplete(transferJob.id, transferJob.exportUrl, transferJob.importUrl);
            window.location.href = transferJob.exportUrl;
        }, transportError);
    }


    reset() {
        this.progressService.reset();
        this.router.navigate([""]);
    }
}
