import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { SelectDataTypeService } from '../select-data-type.service';
import { ServiceDescription, ServiceDescriptions } from '../service-description';
import 'rxjs/add/operator/switchMap';

@Component({
  selector: 'app-import-configuration',
  templateUrl: './import-configuration.component.html',
  styleUrls: ['./import-configuration.component.css']
})
export class ImportConfigurationComponent implements OnInit {
  importServices: ServiceDescription[] = [];
  selectedImportService: string = "";
  error_text: string = "";

  constructor(private service : SelectDataTypeService,
    private route: ActivatedRoute,
    private router: Router,) { }

  ngOnInit() {
    console.log('incoming route param, dataType: ' + this.route.params['type']);

    this.route.params
      .switchMap((params: Params) => this.service.listServices(params['type']))
      .subscribe(
        data => {
          this.importServices = data.importServices;
          this.selectedImportService = data.importServices[0].name;
          console.log('setting importServices: ' + JSON.stringify(this.importServices));
        },
        error => {
          this.error_text = 'There was an error';
          console.error(error);
        }
      );
  }

  // Handles selection of data types
  onSelect(importService: string) {
    console.log('incoming importService: ' + importService);
    // Send to auth
  }
}
