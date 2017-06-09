import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

import { ListServicesComponent } from './list-services/list-services.component';
import { ExportConfigurationComponent } from './export-configuration/export-configuration.component';
import { ImportConfigurationComponent } from './import-configuration/import-configuration.component';

const appRoutes: Routes = [
  { path: 'list-services', component: ListServicesComponent },
  { path: 'export-configuration/:type', component: ExportConfigurationComponent },
  { path: 'import-configuration/:type', component: ImportConfigurationComponent },
  { path: '',
    redirectTo: '/list-services',
    pathMatch: 'full'
  },
  { path: '**', // TODO: Perhaps choose not found
    redirectTo: '/list-services',
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
