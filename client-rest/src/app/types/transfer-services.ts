/**
 * Available transfer services for a given data type.
 */
export interface TransferServices {
    dataType: string;
    exportServices: Array<string>;
    importServices: Array<string>;
}
