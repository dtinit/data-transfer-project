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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import {Observable} from 'rxjs/Rx';

import { DemoComponent } from './demo.component';
import { BackendService } from '../backend.service';
import { PortableDataType } from '../portable-data-type';

class MockBackendService {
  private data_ : PortableDataType = new PortableDataType("dummyName", "dummyDescription");
  listDataTypes(): Observable<any> {
    return Observable.of([this.data_]);
  }
}

describe('DemoComponent', () => {
  let component: DemoComponent;
  let backend: BackendService;
  let fixture: ComponentFixture<DemoComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [ DemoComponent ],
      providers: [ {provide: BackendService, useClass: MockBackendService}]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DemoComponent);
    component = fixture.componentInstance;
    backend = TestBed.get(BackendService);
    spyOn(backend, 'listDataTypes').and.callThrough();
  });

  it('should be created with initial backend call made',() => {
      expect(component).toBeTruthy();
      component.ngOnInit();
      expect(backend.listDataTypes).toHaveBeenCalled();
  });
});
