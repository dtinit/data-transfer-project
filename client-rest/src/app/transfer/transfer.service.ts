import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/index";
import {environment} from "../../environments/environment";
import {CreateTransferJob, GenerateServiceAuthData, ServiceAuthData, ReserveWorker, GetReservedWorker, ReservedWorker, StartTransferJob, TransferJob, TransferServices} from "../types";

@Injectable()
export class TransferService {
    constructor(private http: HttpClient) {
    }

    getServices(dataType: string): Observable<TransferServices> {
        return this.http.get(`${environment.apiBaseUrl}/api/transfer/services/${dataType}`) as Observable<TransferServices>;
    }

    createTransferJob(createTransferJob: CreateTransferJob): Observable<TransferJob> {
        return this.http.post(`${environment.apiBaseUrl}/api/transfer`, createTransferJob) as Observable<TransferJob>;
    }

    generateAuthData(generate: GenerateServiceAuthData): Observable<ServiceAuthData> {
        return this.http.post(`${environment.apiBaseUrl}/api/transfer/${generate.id}/generate`, generate) as Observable<ServiceAuthData>;
    }

    reserveWorker(reserveWorker: ReserveWorker): Observable<string> {
        return this.http.post(`${environment.apiBaseUrl}/api/transfer/worker/${reserveWorker.id}`, reserveWorker) as Observable<string>;
    }

    getReservedWorker(getWorker: GetReservedWorker): Observable<ReservedWorker> {
        return this.http.get(`${environment.apiBaseUrl}/api/transfer/worker/${getWorker.id}`) as Observable<ReservedWorker>;
    }

    startTransferJob(start: StartTransferJob): Observable<TransferJob> {
        return this.http.post(`${environment.apiBaseUrl}/api/transfer/${start.id}/start`, start) as Observable<TransferJob>;
    }
}
