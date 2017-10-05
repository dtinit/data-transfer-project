import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { SelectDataTypeService } from './select-data-type.service';
import { StartCopyComponent } from './start-copy/start-copy.component';
import { DemoComponent } from './demo/demo.component';
import { NextComponent } from './next/next.component';
import { SimpleLoginComponent } from './simplelogin/simplelogin.component';

@NgModule({
  declarations: [
    AppComponent,
    StartCopyComponent,
    DemoComponent,
    NextComponent,
    SimpleLoginComponent
  ],
  imports: [
    AppRoutingModule,
    BrowserModule,
    FormsModule,
    HttpModule,
  ],
  providers: [ SelectDataTypeService ],
  bootstrap: [ AppComponent ]
})
export class AppModule { }
