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
import { PortableDataType } from './portable-data-type';
import { ServiceDescription, ServiceDescriptions } from './service-description';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/map';

interface listServicesResponse {
 export: string[];
 import: string[];
}

interface setupResponse {
 dataType: string;
 importService: string;
 exportService: string;
 importAuthUrl: string;
}

@Injectable()
export class BackendService {
  private baseEndpoint = environment.apiUrl;
  constructor(private http: HttpClient) { }

  listDataTypes() {
    let url = `${this.baseEndpoint}listDataTypes`;
    return this.http.get(url)
      .map(res => this.listDataTypesSuccess(res))
      .catch(err => this.handleError(err));
  }

  listServices(dataType: string) {
    let myParams = new HttpParams().set('dataType', dataType);
    let url = `${this.baseEndpoint}listServices`;
    return this.http.get<listServicesResponse>(url, {params : myParams})
      .map(res => this.listServicesSuccess(res))
      .catch(err => this.handleError(err));
  }

  importSetup() {
    let url = `${this.baseEndpoint}importSetup`;
    return this.http.get<setupResponse>(url)
      .map(res => this.importSetupSuccess(res))
      .catch(err => this.handleError(err));
  }

  copySetup() {
    // copySetup needs to be relative call for XSRF token to be attached
    let url = `/_/copySetup`;
    return this.http.get<setupResponse>(url)
      .map(res => this.copySetupSuccess(res))
      .catch(err => this.handleError(err));
  }

  startCopy() {
    // startCopy needs to be a relative post call for the XSRF token to be included in the header.
    let url = '/_/startCopy';
    return this.http.post(url, '')
      .map(res => this.startCopySuccess(res))
      .catch(err => this.handleError(err));
  }

  private listDataTypesSuccess(res: any) {
    let dataTypes: PortableDataType[] = [];
    for (var prop in res) {
      dataTypes.push(new PortableDataType(res[prop], res[prop]));
    }
    return dataTypes;
  }

  private listServicesSuccess(res: listServicesResponse) {
    let exportServices: ServiceDescription[] = [];
    let exportData = res.export;
    for (var name in exportData) {
      exportServices.push(new ServiceDescription(exportData[name], exportData[name]));
    }

    let importServices: ServiceDescription[] = [];
    let importData = res.import;
    for (var name in importData) {
      importServices.push(new ServiceDescription(importData[name], importData[name]));
    }
    return new ServiceDescriptions(importServices, exportServices);
  }

  private importSetupSuccess(res: setupResponse) {
    let config = new CopyConfiguration(
      res.dataType,
      res.exportService,
      "", // export auth url is not required at this step
      res.importService,
      res.importAuthUrl);
    return config;
  }

  private copySetupSuccess(res: setupResponse) {
    let config = new CopyConfiguration(
      res.dataType,
      res.exportService,
      "", // export auth url is not required at this step
      res.importService,
      ""); // import auth url is not required at this step
    return config;
  }

  private startCopySuccess(res: any) {
    let body = res;
    return body;
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
    console.error(errorMessage);
    return Observable.throw(errorMessage);
  }
}
