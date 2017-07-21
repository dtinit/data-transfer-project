import { Component, OnInit } from '@angular/core';
import { SelectDataTypeService } from '../select-data-type.service';
import { CopyConfiguration } from '../copy-configuration';

@Component({
  selector: 'app-next',
  templateUrl: './next.component.html',
  styleUrls: ['./next.component.css']
})
export class NextComponent implements OnInit {
  config: CopyConfiguration = new CopyConfiguration("", "", "", "", "");
  error_text: string = "";
  constructor(private service : SelectDataTypeService) { }

  ngOnInit() {
    this.importSetup();
  }

  // Fetches data types available from the backend for export and import
  importSetup() {
    this.service.importSetup().subscribe(
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

  // Redirect to the import auth url to authorize
  authorize() {
    console.log('authorize, redirecting to: ' + this.config.importAuthUrl);
    window.location.href=this.config.importAuthUrl;
  }
}
