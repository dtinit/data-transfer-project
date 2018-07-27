import {Injectable} from "@angular/core";

/**
 * Provides stateful management of application data and progress.
 */
@Injectable()
export class ProgressService {
    private appState: AppState;

    constructor() {
        let serialized = sessionStorage.getItem("appstate");
        if (serialized != null) {
            this.appState = JSON.parse(serialized);
        } else {
            this.appState = {
                step: Step.BEGIN,
                transferId: undefined,
                dataType: undefined,
                exportService: undefined,
                exportAuthData: undefined,
                exportUrl: undefined,
                importService: undefined,
                importAuthData: undefined,
                importUrl: undefined,
                workerPublicKey: undefined
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

    exportUrl(): string | undefined {
        return this.appState.exportUrl;
    }

    importAuthData(): string | undefined {
        return this.appState.importAuthData;
    }

    importService(): string | undefined {
        return this.appState.importService;
    }

    importUrl(): string | undefined {
        return this.appState.importUrl;
    }

    workerPublicKey(): string | undefined {
        return this.appState.workerPublicKey;
    }

    private save(): void {
        sessionStorage.setItem("appstate", JSON.stringify(this.appState));
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

    createComplete(transferId: string, exportUrl: string, importUrl: string) {
        this.appState.transferId = transferId;
        this.appState.exportUrl = exportUrl;
        this.appState.importUrl = importUrl;
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

    workerReserved(workerPublicKey: string) {
        this.appState.workerPublicKey = workerPublicKey;
        this.appState.step = Step.WORKER_RESERVED;
        this.save();
    }

    initiated() {
        this.appState.step = Step.RUNNING;
        sessionStorage.removeItem("appstate");
    }

    reset() {
        this.appState.step = Step.BEGIN;
        sessionStorage.removeItem("appstate");
    }

}

interface AppState {
    step: Step;
    transferId: string | undefined;
    dataType: string | undefined
    exportService: string | undefined
    exportUrl: string | undefined
    exportAuthData: string | undefined
    importService: string | undefined
    importAuthData: string | undefined
    importUrl: string | undefined
    workerPublicKey: string | undefined
}


export enum Step {
    BEGIN, DATA, SERVICES, CREATE, AUTHENTICATE_EXPORT, AUTHENTICATE_IMPORT, WORKER_RESERVED, INITIATE, RUNNING, ERROR
}
