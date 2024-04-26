"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const yargs_1 = __importDefault(require("yargs"));
yargs_1.default.scriptName('deck').commandDir('commands', { extensions: ['ts'] }).demandCommand().help().argv;
//# sourceMappingURL=cli.js.map