import {Component, OnInit} from "@angular/core";
import {Router} from "@angular/router";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {EventService} from "../event";
import {ProgressService} from "../progress";
import {TransferService} from "./transfer.service";
import {transportError} from "../transport";

/**
 * Creates a transfer request on the API server.
 */
@Component({
    templateUrl: "./create-transfer.component.html"
})
export class CreateTransferComponent implements OnInit {
    servicesForm: FormGroup;

    dataType: string;
    exportServices: Array<string>;
    importServices: Array<string>;

    constructor(private transferService: TransferService, private progressService: ProgressService, private formBuilder: FormBuilder, private eventService: EventService, private router: Router) {
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
        this.transferService.createTransfer({
            source: this.progressService.exportService(),
            destination: this.progressService.importService(),
            transferDataType: this.progressService.dataType()
        }).subscribe(transfer => {
            this.progressService.createComplete(transfer.id);
            window.location.href = transfer.link;
        }, transportError);
    }


    reset() {
        this.progressService.reset();
        this.router.navigate([""]);
    }
}
