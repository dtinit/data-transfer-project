import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';

import { AppComponent } from './app.component';
import { AppRoutingModule } from './app-routing.module';
import { ListServicesComponent } from './list-services/list-services.component';
import { SelectDataTypeService } from './select-data-type.service';
import { ExportConfigurationComponent } from './export-configuration/export-configuration.component';
import { ImportConfigurationComponent } from './import-configuration/import-configuration.component';
import { StartCopyComponent } from './start-copy/start-copy.component';
import { DemoComponent } from './demo/demo.component';

@NgModule({
  declarations: [
    AppComponent,
    ListServicesComponent,
    ExportConfigurationComponent,
    ImportConfigurationComponent,
    StartCopyComponent,
    DemoComponent
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
