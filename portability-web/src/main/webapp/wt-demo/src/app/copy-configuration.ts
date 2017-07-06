export class CopyConfiguration {
  public dataType: string;
  public exportService: string;
  public exportAuthUrl: string;
  public importService: string;
  public importAuthUrl: string;
  constructor (
    dataType: string,
    exportService: string,
    exportAuthUrl: string,
    importService: string,
    importAuthUrl: string) {
    this.dataType = dataType;
    this.exportService = exportService;
    this.exportAuthUrl = exportAuthUrl;
    this.importService = importService;
    this.importAuthUrl = importAuthUrl;
  }
}
