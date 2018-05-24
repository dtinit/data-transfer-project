/**
 * Available transfer services for a given data type.
 */
export interface TransferServices {

    transferDataType: string;
    exportServices: Array<string>;
    importServices: Array<string>;

}