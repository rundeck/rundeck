"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (k !== "default" && Object.prototype.hasOwnProperty.call(mod, k)) __createBinding(result, mod, k);
    __setModuleDefault(result, mod);
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const Path = __importStar(require("path"));
const ts_rundeck_1 = require("@rundeck/client");
const projectImporter_1 = require("../projectImporter");
class ProjectExportCommand {
    constructor() {
        this.command = "import";
        this.describe = "Run selenium test suite";
    }
    builder(yargs) {
        return yargs
            .option("p", {
            alias: "project",
            require: true,
            describe: "Project name",
            type: 'string'
        })
            .option("r", {
            alias: "repo",
            require: true,
            describe: "Repo path",
            type: 'string'
        })
            .option('u', {
            alias: "url",
            default: "http://127.0.0.1:4440",
            type: "string"
        })
            .option('t', {
            alias: 'testToken',
            describe: 'API Token to use for tests',
            type: 'string'
        });
    }
    async handler(opts) {
        const fullRepoPath = Path.resolve(opts.repo);
        const client = new ts_rundeck_1.Rundeck(opts.testToken ? new ts_rundeck_1.TokenCredentialProvider(opts.testToken) : new ts_rundeck_1.PasswordCredentialProvider(opts.url, 'admin', 'admin'), { baseUri: opts.url });
        const exporter = new projectImporter_1.ProjectImporter(opts.repo, opts.project, client);
        console.log(fullRepoPath);
        await exporter.importProject();
    }
}
module.exports = new ProjectExportCommand();
//# sourceMappingURL=ProjectImportCommand.js.map