import {Injectable} from "@angular/core";
import {EventService} from "../event";
import {ActivatedRoute} from "@angular/router";

/**
 * Provides stateful management of application data and progress.
 */
@Injectable()
export class ProgressService {
    private appState: AppState;

    constructor(private eventService: EventService, activatedRoute: ActivatedRoute) {
        let serialized = localStorage.getItem("appstate");
        if (serialized != null) {
            this.appState = JSON.parse(serialized);
        } else {
            this.appState = {
                step: Step.BEGIN,
                transferId: undefined,
                dataType: undefined,
                importService: undefined,
                importAuthData: undefined,
                exportService: undefined,
                exportAuthData: undefined
            };
            this.save();
        }
    }

    currentStep(): Step {
        return this.appState.step;
    }

    dataType(): string {
        return this.appState.dataType;
    }

    transferId(): string {
        return this.appState.transferId;
    }

    exportAuthData(): string | undefined {
        return this.appState.exportAuthData;
    }

    exportService(): string | undefined {
        return this.appState.exportService;
    }

    importAuthData(): string | undefined {
        return this.appState.importAuthData;
    }

    importService(): string | undefined {
        return this.appState.importService;
    }

    private save(): void {
        localStorage.setItem("appstate", JSON.stringify(this.appState));
    }

    begin() {
        if (this.appState.step < Step.DATA) {
            this.appState.step = Step.DATA;
        }
        this.save();
    }

    dataSelected(dataType: string) {
        this.appState.dataType = dataType;
        if (this.appState.step < Step.SERVICES) {
            this.appState.step = Step.SERVICES;
        }
        this.save();
    }

    servicesSelected(exportService: string, importService: string) {
        this.appState.exportService = exportService;
        this.appState.importService = importService;
        this.appState.step = Step.CREATE;
        this.save();
    }

    createComplete(transferId: string) {
        this.appState.transferId = transferId;
        this.appState.step = Step.AUTHENTICATE_EXPORT;
        this.save();
    }


    authExportComplete(exportAuthData: string) {
        this.appState.exportAuthData = exportAuthData;
        this.appState.step = Step.AUTHENTICATE_IMPORT;
        this.save();
    }

    authImportComplete(importAuthData: string) {
        this.appState.importAuthData = importAuthData;
        this.appState.step = Step.INITIATE;
        this.save();
    }

    initiated() {
        this.appState.step = Step.RUNNING;
        localStorage.removeItem("appstate");
    }

    reset() {
        this.appState.step = Step.BEGIN;
        localStorage.removeItem("appstate");
    }

}

interface AppState {
    step: Step;
    transferId: string | undefined;
    dataType: string | undefined
    exportService: string | undefined
    exportAuthData: string | undefined
    importService: string | undefined
    importAuthData: string | undefined
}


export enum Step {
    BEGIN, DATA, SERVICES, CREATE, AUTHENTICATE_EXPORT, AUTHENTICATE_IMPORT, INITIATE, RUNNING, ERROR
}