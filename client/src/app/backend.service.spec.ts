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
import { TestBed, async, inject } from '@angular/core/testing';
import { HttpClientModule, HttpRequest, HttpParams } from '@angular/common/http';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BackendService } from './backend.service';

describe('BackendService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientModule, HttpClientTestingModule],
      providers: [BackendService]
    });
  });

  afterEach(inject([HttpTestingController], (backend: HttpTestingController) => {
  backend.verify();
}));

  it('should be created', inject([BackendService], (service: BackendService) => {
    expect(service).toBeTruthy();
  }));

  it(`should send an expected login request`, async(inject([BackendService, HttpTestingController],
    (service: BackendService, backend: HttpTestingController) => {
      service.listDataTypes().subscribe();

      backend.expectOne((req: HttpRequest<any>) => {
        const body = new HttpParams({ fromString: req.body });

        return req.url === environment.apiUrl + 'listDataTypes'
          && req.method === 'GET';
      }, `GET to 'listDataTypes'`);
})));

});
