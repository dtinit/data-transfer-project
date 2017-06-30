import { Component, OnInit } from '@angular/core';
import { SelectDataTypeService } from '../select-data-type.service';
import { CopyConfiguration } from '../copy-configuration';

@Component({
  selector: 'app-next',
  templateUrl: './next.component.html',
  styleUrls: ['./next.component.css']
})
export class NextComponent implements OnInit {
  config: CopyConfiguration;
  error_text: string = "";
  constructor(private service : SelectDataTypeService) { }

  ngOnInit() {
    this.fetchCopyConfiguration();
  }

  // Fetches data types available from the backend for export and import
  fetchCopyConfiguration() {
    this.service.fetchCopyConfiguration().subscribe(
      data => {
        this.config = data;
        console.log('config: ' + JSON.stringify(this.config));
      },
      err => {
        this.config = null;
        this.error_text = 'There was an error';
        console.error(err);
      }
    );
  }
}
