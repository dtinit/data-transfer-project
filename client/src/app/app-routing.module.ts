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
import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { DemoComponent } from './demo/demo.component';
import { NextComponent } from './next/next.component';
import { StartCopyComponent } from './start-copy/start-copy.component';
import { SimpleLoginComponent } from './simplelogin/simplelogin.component';


const appRoutes: Routes = [
  { path: 'demo', component: DemoComponent },
  { path: 'next', component: NextComponent },
  { path: 'copy', component: StartCopyComponent },
  { path: 'simplelogin', component: SimpleLoginComponent },
  { path: '',
    redirectTo: '/demo',
    pathMatch: 'full'
  },
  { path: '**', // TODO: Perhaps choose not found
    redirectTo: '/demo',
    pathMatch: 'full'
  },
];

@NgModule({
  imports: [
    RouterModule.forRoot(appRoutes)
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule { }
