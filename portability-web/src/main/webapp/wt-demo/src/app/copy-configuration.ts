export class CopyConfiguration {
  public dataType: string;
  public exportService: string;
  public exportServiceAuthExists: boolean;
  public exportAuthUrl: string;
  public importService: string;
  public importServiceAuthExists: boolean;
  public importAuthUrl: string;
  constructor (
    dataType: string,
    exportService: string,
    exportServiceAuthExists: boolean,
    exportAuthUrl: string,
    importService: string,
    importServiceAuthExists: boolean,
    importAuthUrl: string) {
    this.dataType = dataType;
    this.exportService = exportService;
    this.exportServiceAuthExists = exportServiceAuthExists;
    this.exportAuthUrl = exportAuthUrl;
    this.importService = importService;
    this.importServiceAuthExists = importServiceAuthExists;
    this.importAuthUrl = importAuthUrl;
  }
}
