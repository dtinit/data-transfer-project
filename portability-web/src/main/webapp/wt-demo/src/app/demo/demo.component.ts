import { Component, OnInit } from '@angular/core';
import { SelectDataTypeService } from '../select-data-type.service';
import { ServiceDescription, ServiceDescriptions } from '../service-description';
import { PortableDataType } from '../portable-data-type';


@Component({
  selector: 'app-demo',
  templateUrl: './demo.component.html',
  styleUrls: ['./demo.component.css']
})
export class DemoComponent implements OnInit {
  selectedDataType: string = "";
  dataTypes: PortableDataType[] = [];
  exportServices: ServiceDescription[] = [];
  selectedExportService: string = "";
  importServices: ServiceDescription[] = [];
  selectedImportService: string = "";
  showImport: boolean = false;
  enableNext: boolean = false;
  error_text: string = "";
  constructor(private service : SelectDataTypeService) { }

  ngOnInit() {
    this.toggleImport(false);
    this.toggleNext(false);
    this.fetchAvailableDataTypes();
  }

  // Fetches data types available from the backend for export and import
  fetchAvailableDataTypes() {
    this.service.listDataTypes().subscribe(
      res => {
        this.dataTypes = res;
        console.log('updated dataTypes: ' + JSON.stringify(this.dataTypes));
      },
      err => {
        this.dataTypes = [];
        this.error_text = 'There was an error';
        console.error(err);
      }
    );
  }

  // Fetches import and export service list from the backend for the selected data type
  fetchServices() {
    this.service.listServices(this.selectedDataType).subscribe(
      data => {
        this.exportServices = data.exportServices;
        this.importServices = data.importServices;
        console.log('setting exportServices: ' + JSON.stringify(this.exportServices));
        console.log('setting importServices: ' + JSON.stringify(this.importServices));
      },
      error => {
        this.error_text = 'There was an error';
        console.error(error);
      }
    );
  }

  // Handles selection of data types
  onDataTypeChange() {
    console.log('selectedDataType: ' + this.selectedDataType);
    this.toggleImport(false);
    this.toggleNext(false);
    this.fetchServices();
  }

  // Handles selection of an export service
  onExportServiceChange() {
    this.toggleImport(true); // Ensure import is shown anytime export is chosen
    this.toggleNext(false);
  }

  // Handles selection of an export service
  onImportServiceChange() {
    this.toggleNext(true); // Ensure next is enabled anytime import is chosen
  }

  // Toggle showing import
  private toggleImport(show: boolean) {
    if(this.showImport != show) {
      this.showImport = show;
    }
  }

  // Toggle showing import
  private toggleNext(show: boolean) {
    if(this.enableNext != show) {
      this.enableNext = show;
    }
  }
}
