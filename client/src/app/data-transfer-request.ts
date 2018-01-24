// Represents the configuration for a data transfer job
export class DataTransferRequest {
constructor (
  public transferDataType: string,
  public source: string,
  public destination: string){}
}