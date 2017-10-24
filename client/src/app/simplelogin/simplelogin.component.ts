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
import { environment } from '../../environments/environment';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-simplelogin',
  templateUrl: './simplelogin.component.html',
  styleUrls: ['./simplelogin.component.css']
})
export class SimpleLoginComponent implements OnInit {
  url = environment.apiPostUrl;
  username: string = "";
  password: string = "";
  enableSubmit: boolean = false;
  error_text: string = "";
  constructor() { }

  ngOnInit() {
    this.toggleSubmit(false);
  }

  // Handles change in text input value
  onInputChange() {
    console.log('username: ' + this.username);
    console.log('password: ' + this.password);
    if(this.password.length > 2 && this.username.length > 2) {
      this.toggleSubmit(true);
    }
  }

  // Toggle showing submit
  private toggleSubmit(show: boolean) {
    if(this.enableSubmit != show) {
      this.enableSubmit = show;
    }
  }
}
