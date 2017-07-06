import { Component, OnInit } from '@angular/core';
import { SelectDataTypeService } from '../select-data-type.service';

@Component({
  selector: 'app-start-copy',
  templateUrl: './start-copy.component.html',
  styleUrls: ['./start-copy.component.css']
})
export class StartCopyComponent implements OnInit {
  dataType: string = "";
  exportService: string = "";
  importService: string = "";
  submitted: boolean = false;
  error_text: string = "";
  constructor(private service : SelectDataTypeService) { }

  ngOnInit() {
    console.log('ngOnInit for startCopySetup');
    this.copySetup();
  }

  // List copy configuration, e.g. services selected for export and import
  copySetup() {
    this.service.copySetup().subscribe(
      data => {
        this.dataType = data.dataType;
        this.exportService = data.exportService;
        this.importService = data.importService;
        console.log('copySetup: dataType: ' + this.dataType);
        console.log('copySetup: exportService: ' + this.exportService);
        console.log('copySetup: importService: ' + this.importService);
      },
      err => {
        this.dataType = '';
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
        this.submitted = true;
        console.log('successfully called startCopy, data: ' + data);
      },
      err => {
        this.error_text = 'There was an error calling startCopy';
        console.error(err);
      }
    );
  }
}
