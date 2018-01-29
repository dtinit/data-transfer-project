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
import { environment } from '../environments/environment';
import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse, HttpParams, HttpErrorResponse } from '@angular/common/http'
import { Observable } from 'rxjs/Observable';
import { CopyConfiguration } from './copy-configuration';
import { DataTransferRequest } from './data-transfer-request';
import { DataTransferResponse } from './data-transfer-response';
import { ListDataTypesResponse } from './list-data-types-response';
import { PortableDataType } from './portable-data-type';
import { ServiceDescription, ServiceDescriptions } from './service-description';
import { ListServicesResponse } from './list-services-response';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/map';

@Injectable()
export class BackendService {
  private baseEndpoint = environment.apiUrl;
  private apiEndpoint = environment.apiPostUrl;
  constructor(private http: HttpClient) { }

  listDataTypes() {
    let url = `${this.baseEndpoint}listDataTypes`;
    return this.http.get<ListDataTypesResponse>(url)
      .catch(err => this.handleError(err));
  }

  listServices(dataType: string) {
    let myParams = new HttpParams().set('dataType', dataType);
    let url = `${this.baseEndpoint}listServices`;
    return this.http.get<ListServicesResponse>(url, {params : myParams})
      .catch(err => this.handleError(err));
  }

  dataTransfer(formData: DataTransferRequest) {
    let url = '/_/DataTransfer';
    this.http.post<DataTransferResponse>(url, JSON.stringify(formData))
        .map(res=>this.configureSuccess(res))
        .catch(err=>this.handleError(err))
        .subscribe();
  }

  importSetup() {
    let url = `${this.baseEndpoint}importSetup`;
    return this.http.get<DataTransferResponse>(url)
      .catch(err => this.handleError(err));
  }

  copySetup() {
    // copySetup needs to be relative call for XSRF token to be attached
    let url = `/_/copySetup`;
    return this.http.get<DataTransferResponse>(url)
      .catch(err => this.handleError(err));
  }

  startCopy() {
    // startCopy needs to be a relative post call for the XSRF token to be included in the header.
    let url = '/_/startCopy';
    return this.http.post(url, '')
      .map(res => this.startCopySuccess(res))
      .catch(err => this.handleError(err));
  }

  private startCopySuccess(res: any) {
    let body = res;
    return body;
  }

  private configureSuccess(res:DataTransferResponse){
    // Redirect to the export authorization flow after configure.
    // this should be returned from the configure request and is checked
    // upon creation of the DataTransfer object.
    console.log("DataTransferResponse, redirecting to: " + res.nextUrl)
    window.location.href = res.nextUrl;
  }

  private handleError(error: HttpErrorResponse | any) {
    // In a real world app, you might use a remote logging infrastructure
    let errorMessage: string;
    if (error.error instanceof Error) {
      const err = error.error.message || JSON.stringify(error);
      errorMessage = `${error.status} - ${error.statusText || ''} ${err}`;
    } else {
      errorMessage = error.message ? error.message : error.toString();
    }
    console.error(error);
    return Observable.throw(error);
  }
}
