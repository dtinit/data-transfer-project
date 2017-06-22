import { Component, OnInit } from '@angular/core';
import { SelectDataTypeService } from '../select-data-type.service';

@Component({
  selector: 'app-start-copy',
  templateUrl: './start-copy.component.html',
  styleUrls: ['./start-copy.component.css']
})
export class StartCopyComponent implements OnInit {
  exportService: string = "";
  importService: string = "";
  error_text: string = "";
  constructor(private service : SelectDataTypeService) { }

  ngOnInit() {
    console.log('ngOnInit for fetchCopyConfiguration');
    this.fetchCopyConfiguration();
  }

  // List copy configuration, e.g. services selected for export and import
  fetchCopyConfiguration() {
    this.service.fetchCopyConfiguration().subscribe(
      data => {
        this.exportService = data.export;
        this.importService = data.import;
        console.log('fetchCopyConfiguration: exportService: ' + this.exportService);
        console.log('fetchCopyConfiguration: importService: ' + this.importService);
      },
      err => {
        this.exportService = '';
        this.importService = '';
        this.error_text = 'There was an error';
        console.error(err);
      }
    );
  }

  // Handles the users initiation of the copy
  onSelect() {
    console.log('copy clicked');
    // Initiate the copy
    this.service.startCopy().subscribe(
      data => {
        console.log('successfully called startCopy, data: ' + data);
      },
      err => {
        this.error_text = 'There was an error calling startCopy';
        console.error(err);
      }
    );
  }
}
