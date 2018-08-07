import {RouterModule, Routes} from "@angular/router";
import {StartComponent} from "./start";
import {DataComponent} from "./data";
import {ProgressGuard} from "./progress";
import {AuthCallbackComponent, CreateTransferComponent} from "./transfer";
import {InitiateTransferComponent} from "./transfer/initiate-transfer.component";

const appRoutes: Routes = [
    {path: "", component: StartComponent},
    {path: "data", component: DataComponent, canActivate: [ProgressGuard]},
    {path: "create", component: CreateTransferComponent, canActivate: [ProgressGuard]},
    {path: "initiate", component: InitiateTransferComponent, canActivate: [ProgressGuard]},
    {path: "auth", component: AuthCallbackComponent},
    {path: "callback/:service", component: StartComponent},

    // otherwise redirect to start
    {path: "**", redirectTo: ""}
];

export const routing = RouterModule.forRoot(appRoutes);


