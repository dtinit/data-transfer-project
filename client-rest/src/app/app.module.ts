import {RouterTestingModule} from '@angular/router/testing';
import {BrowserModule} from "@angular/platform-browser";
import {NgModule} from "@angular/core";

import {AppComponent} from "./app.component";
import {HttpClientModule} from "@angular/common/http";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {routing} from "./app.routing";
import {StartComponent} from "./start";
import {DataComponent, DataTypesService} from "./data";
import {ProgressComponent, ProgressGuard, ProgressService} from "./progress";
import {CommonModule, TitleCasePipe} from "@angular/common";
import {AuthCallbackComponent, CreateTransferComponent, TransferService} from "./transfer";
import {InitiateTransferComponent} from "./transfer/initiate-transfer.component";

@NgModule({
    declarations: [
        AppComponent, ProgressComponent, AuthCallbackComponent, StartComponent, DataComponent, CreateTransferComponent, InitiateTransferComponent
    ],
    imports: [
        BrowserModule, HttpClientModule, FormsModule, ReactiveFormsModule, CommonModule, routing, RouterTestingModule,
    ],
    providers: [ProgressService, DataTypesService, TransferService, ProgressGuard, TitleCasePipe],
    bootstrap: [AppComponent]
})
export class AppModule {
}
