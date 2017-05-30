export class ServiceDescription {
  public name: string;
  public description: string;
  constructor (name: string, description: string) {
    this.name = name;
    this.description = description;
  }
}

export class ServiceDescriptions {
  public importServices: ServiceDescription[];
  public exportServices: ServiceDescription[];
  constructor (importServices: ServiceDescription[], exportServices: ServiceDescription[]) {
    this.importServices = importServices;
    this.exportServices = exportServices;
  }
}
