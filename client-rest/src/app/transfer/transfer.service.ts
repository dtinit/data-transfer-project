import {Injectable} from "@angular/core";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/index";
import {environment} from "../../environments/environment";
import {CreateTransfer, GenerateServiceAuthData, ServiceAuthData, StartTransfer, Transfer, TransferServices} from "../types";

@Injectable()
export class TransferService {


    constructor(private http: HttpClient) {
    }

    getServices(dataType: string): Observable<TransferServices> {
        return this.http.get(`${environment.apiUrl}/transfer/services/${dataType}`) as Observable<TransferServices>;
    }

    createTransfer(createTransfer: CreateTransfer): Observable<Transfer> {
        return this.http.post(`${environment.apiUrl}/transfer`, createTransfer) as Observable<Transfer>;
    }

    prepareImport(id: string): Observable<Transfer> {
        return this.http.post(`${environment.apiUrl}/transfer/${id}/import`, {}) as Observable<Transfer>;
    }

    initiateTransfer(start: StartTransfer): Observable<Transfer> {
        return this.http.post(`${environment.apiUrl}/transfer/${start.id}/start`, start) as Observable<Transfer>;
    }

    generateAuthData(generate: GenerateServiceAuthData): Observable<ServiceAuthData> {
        return this.http.post(`${environment.apiUrl}/transfer/${generate.id}/generate`, generate) as Observable<ServiceAuthData>;
    }
}