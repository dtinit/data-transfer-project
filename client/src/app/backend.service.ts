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
import { Http, Headers, Response, RequestOptions, URLSearchParams } from '@angular/http';
import { Observable } from 'rxjs/Observable';
import { CopyConfiguration } from './copy-configuration';
import { PortableDataType } from './portable-data-type';
import { ServiceDescription, ServiceDescriptions } from './service-description';
import 'rxjs/add/operator/catch';
import 'rxjs/add/operator/map';

@Injectable()
export class BackendService {
  private baseEndpoint = environment.apiUrl;
  constructor(private http: Http) { }

  listDataTypes() {
    let url = `${this.baseEndpoint}listDataTypes`;
    return this.http.get(url)
      .map(res => this.listDataTypesSuccess(res))
      .catch(err => this.handleError(err));
  }

  listServices(dataType: string) {
    let myParams = new URLSearchParams();
    myParams.append('dataType', dataType);
    let options = new RequestOptions({ params: myParams});
    let url = `${this.baseEndpoint}listServices`;
    return this.http.get(url, options)
      .map(res => this.listServicesSuccess(res))
      .catch(err => this.handleError(err));
  }

  importSetup() {
    let url = `${this.baseEndpoint}importSetup`;
    return this.http.get(url)
      .map(res => this.importSetupSuccess(res))
      .catch(err => this.handleError(err));
  }

  copySetup() {
    let url = `${this.baseEndpoint}copySetup`;
    return this.http.get(url)
      .map(res => this.copySetupSuccess(res))
      .catch(err => this.handleError(err));
  }

  startCopy() {
    let url = `${this.baseEndpoint}startCopy`;
    let headers = new Headers({}); // request is empty as it relies on data in cookies.
    let options = new RequestOptions({ headers : headers });
    return this.http.post(url, '', options)
      .map(res => this.startCopySuccess(res))
      .catch(err => this.handleError(err));
  }

  private listDataTypesSuccess(res: Response) {
    let body = res.json();
    let dataTypes: PortableDataType[] = [];
    for (var prop in body) {
      dataTypes.push(new PortableDataType(body[prop], body[prop]));
    }
    return dataTypes;
  }

  private listServicesSuccess(res: Response) {
    let body = res.json();

    let exportServices: ServiceDescription[] = [];
    let exportData = body['export'];
    for (var name in exportData) {
      exportServices.push(new ServiceDescription(exportData[name], exportData[name]));
    }

    let importServices: ServiceDescription[] = [];
    let importData = body['import'];
    for (var name in importData) {
      importServices.push(new ServiceDescription(importData[name], importData[name]));
    }

    return new ServiceDescriptions(importServices, exportServices);
  }

  private importSetupSuccess(res: Response) {
    let data = res.json();
    let config = new CopyConfiguration(
      data.dataType,
      data.exportService,
      "", // export auth url is not required at this step
      data.importService,
      data.importAuthUrl);

    return config;
  }

  private copySetupSuccess(res: Response) {
    let data = res.json();
    let config = new CopyConfiguration(
      data.dataType,
      data.exportService,
      "", // export auth url is not required at this step
      data.importService,
      ""); // import auth url is not required at this step
    return config;
  }

  private startCopySuccess(res: Response) {
    let body = res.json();
    return body;
  }

  private handleError(error: Response | any) {
    // In a real world app, you might use a remote logging infrastructure
    let errorMessage: string;
    if (error instanceof Response) {
      const body = error.json() || '';
      const err = body.error || JSON.stringify(body);
      errorMessage = `${error.status} - ${error.statusText || ''} ${err}`;
    } else {
      errorMessage = error.message ? error.message : error.toString();
    }
    console.error(errorMessage);
    return Observable.throw(errorMessage);
  }
}
