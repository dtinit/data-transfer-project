import { Component, EventEmitter, Input, OnInit, Output} from '@angular/core';
import { SelectDataTypeService } from '../select-data-type.service';
import { PortableDataType } from '../portable-data-type';
import { ServiceDescriptions } from '../service-description';

@Component({
  selector: 'app-list-services',
  templateUrl: './list-services.component.html',
  styleUrls: ['./list-services.component.css']
})
export class ListServicesComponent implements OnInit {
  selectedDataType: string = 'Please make a selection';
  availableOptions: PortableDataType[] = [];
  @Output() onDataTypeSelection = new EventEmitter<ServiceDescriptions>();
  dataTypeSelect: any;
  error_text: string = "";

  constructor(private selectDataTypeService : SelectDataTypeService) { }

  ngOnInit() {
    this.listDataTypes();
  }

  // List data types available for export and import
  listDataTypes() {
    // TODO: Fetch the data types from the backend
    this.selectDataTypeService.listDataTypes().subscribe(
      data => {
        this.availableOptions = data;
        console.log('updated availableOptions: ' + JSON.stringify(this.availableOptions));
      },
      error => {
        this.availableOptions = [];
        this.error_text = 'There was an error';
        console.error(error);
      }
    );
  }

  // List services available for export and import
  listServicesForDataType(dataType: string) {
    console.log('incoming dataType: ' + dataType);
    // TODO: Fetch the data types from the backend
    this.selectDataTypeService.listServices(dataType).subscribe(
      data => {
        this.onDataTypeSelection.emit(new ServiceDescriptions(data.exportServices, data.importServices))
      },
      error => {
        this.error_text = 'There was an error';
        console.error(error);
      }
    );
  }
}
