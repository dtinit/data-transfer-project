import {Component, ElementRef, OnInit, ViewChild} from "@angular/core";
import {Router} from "@angular/router";
import {DataTypesService} from "./data-types.service";
import {FormBuilder, FormGroup, Validators} from "@angular/forms";
import {ProgressService} from "../progress";
import {transportError} from "../transport";

/**
 * Allows a user to select the type of data to transfer, e.g. contacts, photos, etc., from a list of available types.
 */
@Component({
    templateUrl: "./data.component.html"
})
export class DataComponent implements OnInit {
    dataTypes: Array<string>;
    dataForm: FormGroup;

    @ViewChild("dataType") private dataTypeRef: ElementRef;

    constructor(private dataTypesService: DataTypesService, private formBuilder: FormBuilder, private progressService: ProgressService, private router: Router) {
        this.dataForm = this.formBuilder.group({
            dataType: [undefined, Validators.required]
        });
    }

    ngOnInit() {
        this.dataTypesService.getDataTypes().subscribe((dataTypes) => {
            this.dataTypes = dataTypes.dataTypes.sort();
            this.dataForm.get("dataType").setValue(this.dataTypes[0]);
            this.dataTypeRef.nativeElement.focus();
        }, transportError);
    }

    next() {
        this.progressService.dataSelected(this.dataForm.get("dataType").value);
        this.router.navigate(["create"]);
    }

    reset() {
        this.progressService.reset();
        this.router.navigate([""]);
    }

}
