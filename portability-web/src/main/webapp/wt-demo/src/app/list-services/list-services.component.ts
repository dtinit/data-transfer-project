import { Component, Input, OnInit} from '@angular/core';
import { Router }  from '@angular/router';
import { SelectDataTypeService } from '../select-data-type.service';
import { PortableDataType } from '../portable-data-type';

@Component({
  templateUrl: './list-services.component.html',
  styleUrls: ['./list-services.component.css']
})
export class ListServicesComponent implements OnInit {
  selectedDataType: string = '';
  dataTypes: PortableDataType[] = [];
  dataTypeSelect: any;
  error_text: string = "";

  constructor(private router: Router,
    private service : SelectDataTypeService) { }

  ngOnInit() {
    // Populate the initial page with available data types
    // TODO: Consider caching list
    this.fetchAvailableDataTypes();
  }

  // List data types available for export and import
  fetchAvailableDataTypes() {
    // TODO: Fetch the data types from the backend
    this.service.listDataTypes().subscribe(
      data => {
        this.dataTypes = data;
        this.selectedDataType = data[0].name;
        console.log('updated dataTypes: ' + JSON.stringify(this.dataTypes));
      },
      error => {
        this.dataTypes = [];
        this.error_text = 'There was an error';
        console.error(error);
      }
    );
  }

  // Handles selection of data types
  onSelect(dataType: string) {
    console.log('incoming dataType: ' + dataType);
    this.router.navigate(['/export-configuration', dataType]);
  }
}
