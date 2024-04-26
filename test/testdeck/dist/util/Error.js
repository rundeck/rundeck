"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CustomError = void 0;
/** A custom Error class than can have extra error stacks appended */
class CustomError extends Error {
    constructor(message) {
        super(message);
    }
    addCause(cause) {
        this.stack = `${this.stack}\n\nCaused by: ${cause.stack}`;
    }
}
exports.CustomError = CustomError;
//# sourceMappingURL=Error.js.map