export function transportError(error: any) {
    console.error(error);
    alert(`Sorry, something is not right.\n\nCode: ${error.status}\nMessage: ${error.message}`);
}