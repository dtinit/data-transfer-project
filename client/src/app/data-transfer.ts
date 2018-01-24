// Represents the configuration for a data transfer job
export class DataTransfer {
  constructor (
  public transferDataType: string,
  public source: string,
  public destination: string,
  public nextURL: string){}
}