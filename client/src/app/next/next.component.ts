/*
 * Copyright 2018 The Data Transfer Project Authors.
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
import { FormsModule } from '@angular/forms';
import { BackendService } from '../backend.service';
import { CopyConfiguration } from '../copy-configuration';
import { CreateJobResponse } from '../create-job-response';

@Component({
  selector: 'app-next',
  templateUrl: './next.component.html',
  styleUrls: ['./next.component.css']
})
export class NextComponent implements OnInit {
  // This is kinda ugly but is the only way to init an interface wihtout creating a dummy class
  createJobResponse: CreateJobResponse = <CreateJobResponse>{transferDataType:"", source:"", destination:""};
  error_text: string = "";
  constructor(private service : BackendService) { }

  ngOnInit() {
    this.importSetup();
  }

  // Fetches data types available from the backend for export and import
  importSetup() {
    this.service.importSetup().subscribe(
      data => {
        this.createJobResponse = data;
        console.log('createJobResponse: ' + JSON.stringify(this.createJobResponse));
      },
      err => {
        this.createJobResponse = null;
        this.error_text = 'There was an error';
        console.error(err);
      }
    );
  }

  // Redirect to the import auth url to authorize
  authorize() {
    console.log('authorize, redirecting to: ' + this.createJobResponse.nextUrl);
    window.location.href = this.createJobResponse.nextUrl;
  }
}
