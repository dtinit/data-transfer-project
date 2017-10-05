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
