/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import { Component, OnInit } from '@angular/core';
import { BackendService } from '../backend.service';
import { DataTransferResponse } from '../data-transfer-response';

@Component({
  selector: 'app-start-copy',
  templateUrl: './start-copy.component.html',
  styleUrls: ['./start-copy.component.css']
})
export class StartCopyComponent implements OnInit {
  dataTransferResponse: DataTransferResponse = <DataTransferResponse>{transferDataType:"", source:"", destination:""};
  submitted: boolean = false;
  error_text: string = "";
  constructor(private service : BackendService) { }

  ngOnInit() {
    console.log('ngOnInit for startCopySetup');
    this.copySetup();
  }

  // List copy configuration, e.g. services selected for export and import
  copySetup() {
    this.service.copySetup().subscribe(
      data => {
        this.dataTransferResponse = data;
        console.log('copySetup: dataTransferResponse: ' + JSON.stringify(this.dataTransferResponse));
      },
      err => {
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
