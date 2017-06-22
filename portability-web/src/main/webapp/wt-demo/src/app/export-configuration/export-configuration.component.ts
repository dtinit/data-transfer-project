import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, Params } from '@angular/router';
import { SelectDataTypeService } from '../select-data-type.service';
import { ServiceDescription, ServiceDescriptions } from '../service-description';
import 'rxjs/add/operator/switchMap';

@Component({
  templateUrl: './export-configuration.component.html',
  styleUrls: ['./export-configuration.component.css']
})

export class ExportConfigurationComponent implements OnInit {
  exportServices: ServiceDescription[] = [];
  selectedExportService: string = "";
  error_text: string = "";

  constructor(private service : SelectDataTypeService,
    private route: ActivatedRoute,
    private router: Router,) { }

  ngOnInit() {
    console.log('incoming route param, dataType: ' + this.route.params['dataType']);

    // TODO: Currently supports incoming from the previous page only. Consider
    // supporting preloading information from the backend lookup
    this.route.params
      .switchMap((params: Params) => this.service.listServices(params['dataType']))
      .subscribe(
        data => {
          this.exportServices = data.exportServices;
          if (data.selectedExportService) {
            this.selectedExportService = data.selectedExportService;
          } else {
            this.selectedExportService = data.exportServices[0].name;
          }
          console.log('setting exportServices: ' + JSON.stringify(this.exportServices));
        },
        error => {
          this.error_text = 'There was an error';
          console.error(error);
        }
      );
  }

  // Handles selection of the service to export from
  onSelect(exportService: string) {
    console.log('incoming exportService: ' + exportService);
    // Fetch the redirect url for authentication from the backend
    this.service.selectExportService(exportService).subscribe(
      data => {
        console.log('successfully called selectExportService, data: ' + data);
        window.location.href = data;
      },
      err => {
        this.error_text = 'There was an error calling selectExportService';
        console.error(err);
      }
    );
  }
}
