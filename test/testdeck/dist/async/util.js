"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.prompt = exports.sleep = void 0;
const readline_1 = __importDefault(require("readline"));
function sleep(ms) {
    return new Promise(res => {
        setTimeout(res, ms);
    });
}
exports.sleep = sleep;
function prompt(prompt) {
    const rl = readline_1.default.createInterface({
        input: process.stdin,
        output: process.stdout
    });
    return new Promise((res) => {
        rl.question(prompt, (answer) => res(answer));
    });
}
exports.prompt = prompt;
//# sourceMappingURL=util.js.map