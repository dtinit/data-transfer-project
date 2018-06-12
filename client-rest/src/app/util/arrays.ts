export class Arrays {
    /**
     * Removes an element from the array.
     * @param element the element
     * @param array the array
     * @returns {boolean} true if the element was removed
     */
    static removeElement = function (element: any, array: Array<any>) {
        let index = array.indexOf(element);
        if (index > -1) {
            array.splice(index, 1);
            return true;
        }
        return false;
    };

}