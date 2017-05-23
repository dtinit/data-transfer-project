import { Component, OnInit, Input } from '@angular/core';
import { SelectDataTypeService } from '../select-data-type.service';
import { ServiceDescription, ServiceDescriptions } from '../service-description';

@Component({
  selector: 'app-export-configuration',
  templateUrl: './export-configuration.component.html',
  styleUrls: ['./export-configuration.component.css']
})

export class ExportConfigurationComponent implements OnInit {
  exportServices: ServiceDescription[] = [];
  importServices: ServiceDescription[] = [];
  exportService: string = "";
  importService: string = "";
  error_text: string = "";

  constructor(private selectDataTypeService : SelectDataTypeService) { }

  ngOnInit() { }

  @Input()
  set services(services: ServiceDescriptions) {
    console.log("ExportConfigurationComponent:serviceDescriptions: " + JSON.stringify(services));
    this.exportServices = services.exportServices;
    this.importServices = services.importServices;
  }

  // TODO implements
  authorizeExport(exportService: string, importService: string) {
    this.exportService = exportService;
    this.importService = importService;
    console.log("Selected exportSevice: " + exportService);
    console.log("Selected importService: " + importService);
  }
}
