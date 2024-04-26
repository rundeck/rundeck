/** A custom Error class than can have extra error stacks appended */
export declare class CustomError extends Error {
    constructor(message: string);
    addCause(cause: Error): void;
}
