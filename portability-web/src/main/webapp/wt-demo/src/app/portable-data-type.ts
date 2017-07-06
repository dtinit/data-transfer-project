// Represents the name and description for a data type
export class PortableDataType {
  public name: string;
  public description: string;
  constructor (name: string, description: string) {
    this.name = name;
    this.description = description;
  }
}
