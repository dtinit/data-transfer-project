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
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {Observable} from 'rxjs/Rx';
import { FormsModule } from '@angular/forms';
import { BackendService } from '../backend.service';

import { SimpleLoginComponent } from './simplelogin.component';

class MockBackendService {
  submitSimpleCreds(): Observable<any> {
    return Observable.of();
  }
}

describe('SimpleLoginComponent', () => {
  let component: SimpleLoginComponent;
  let backend: BackendService;
  let fixture: ComponentFixture<SimpleLoginComponent>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [FormsModule],
      declarations: [ SimpleLoginComponent ],
      providers: [ {provide: BackendService, useClass: MockBackendService}]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SimpleLoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should be created', () => {
    expect(component).toBeTruthy();
  });
});
