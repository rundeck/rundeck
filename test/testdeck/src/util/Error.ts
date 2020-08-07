/** A custom Error class than can have extra error stacks appended */
export class CustomError extends Error {
    constructor(message: string) {
        super(message)
    }

    addCause(cause: Error) {
        this.stack = `${this.stack}\n\nCaused by: ${cause.stack}`
    }
}