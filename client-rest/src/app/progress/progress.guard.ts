import {Injectable} from "@angular/core";
import {ActivatedRouteSnapshot, CanActivate, RouterStateSnapshot} from "@angular/router";
import {ProgressService, Step} from "./progress.service";

/**
 * Disables invalid application transitions.
 */
@Injectable()
export class ProgressGuard implements CanActivate {

    constructor(private progressService: ProgressService) {
    }

    canActivate(route: ActivatedRouteSnapshot, state: RouterStateSnapshot) {
        if ("/create" == state.url && this.progressService.currentStep() < Step.SERVICES) {
            return false;
        } else if ("/import" == state.url && this.progressService.currentStep() < Step.AUTHENTICATE_EXPORT) {
            return false;
        } else if ("/initiate" == state.url && this.progressService.currentStep() < Step.WORKER_RESERVED) {
            return false;
        }
        return true;
    }
}
