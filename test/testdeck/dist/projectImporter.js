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
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProjectImporter = exports.KeyType = void 0;
const Path = __importStar(require("path"));
const models_1 = require("@rundeck/client/dist/lib/models");
const TmpP = __importStar(require("tmp"));
const js_yaml_1 = __importDefault(require("js-yaml"));
const child_process_1 = require("./async/child-process");
const Tmp = __importStar(require("./async/tmp"));
const FS = __importStar(require("./async/fs"));
var KeyType;
(function (KeyType) {
    KeyType["Public"] = "publicKey";
    KeyType["Private"] = "privateKey";
    KeyType["Password"] = "password";
})(KeyType || (exports.KeyType = KeyType = {}));
TmpP.setGracefulCleanup();
class ProjectImporter {
    constructor(repoPath, projectName, client) {
        this.repoPath = repoPath;
        this.projectName = projectName;
        this.client = client;
    }
    async importProject() {
        const dir = await Tmp.dir({ prefix: 'demo_projects_' });
        const importFileName = Path.join(dir, `${this.projectName}.zip`);
        const projectDir = Path.join(this.repoPath, this.projectName);
        // console.log('Creating project archive...')
        // console.log(chalk.blue(this.projectName))
        await (0, child_process_1.exec)(`cd ${projectDir} && zip -r ${importFileName} *`);
        const projectList = await this.client.projectList();
        /* Create project in instance if it does not exist. */
        if (projectList.filter(p => p.name == this.projectName).length == 0) {
            // console.log('Creating project...')
            await this.client.projectCreate({ name: this.projectName });
        }
        /**
         * Project import
         */
        // console.log('Importing project...')
        await this.client.projectArchiveImport(this.projectName, await FS.readFile(importFileName), { importConfig: true, importACL: true, jobUuidOption: models_1.JobUuidOption.Preserve });
        // console.log('Importing readme...')
        const readmePath = Path.join(projectDir, `rundeck-${this.projectName}/files/readme.md`);
        if (await FS.exists(readmePath)) {
            const readme = await FS.readFile(readmePath);
            await this.client.projectReadmePut(this.projectName, { contents: readme.toString() });
        }
        // console.log('Importing mod...')
        const motdPath = Path.join(projectDir, `rundeck-${this.projectName}/files/motd.md`);
        if (await FS.exists(motdPath)) {
            const motd = await FS.readFile(motdPath);
            await this.client.projectMotdPut(this.projectName, { contents: motd.toString() });
        }
        /**
         * Key import
         */
        const keyRepoPath = Path.join(projectDir, 'system/keys');
        if (await FS.exists(keyRepoPath)) {
            // console.log('Importing keys...')
            const keyFiles = (await (0, child_process_1.exec)(`find ${keyRepoPath} -type f`)).stdout.toString().trim().split('\n');
            const keys = [];
            for (const file of keyFiles) {
                keys.push(js_yaml_1.default.safeLoad((await FS.readFile(file)).toString()));
            }
            for (const key of keys) {
                // console.log(chalk.blue(key.path))
                let contentType = 'application/pgp-keys';
                switch (key.type) {
                    case 'password':
                        contentType = 'application/x-rundeck-data-password';
                }
                const [path, file] = [key.path.split('/').slice(1).join('/'), key.value];
                const resp = await this.client.storageKeyCreate(path, file, { contentType });
                if (resp._response.status == 409)
                    await this.client.storageKeyUpdate(path, file, { contentType });
            }
        }
        /**
         * ACL import
         */
        const aclRepoPath = Path.join('projects', this.projectName, 'system/acls');
        if (await FS.exists(aclRepoPath)) {
            // console.log('Importing system ACLs...')
            const aclFiles = (await (0, child_process_1.exec)(`find ${aclRepoPath} -type f`)).stdout.toString().trim().split('\n');
            const aclList = await this.client.systemAclPolicyList();
            const aclNames = aclList.resources.map(a => a.name);
            for (const file of aclFiles) {
                const aclName = Path.basename(file).split('.').slice(0, -1).join('.');
                const aclContents = await FS.readFile(file);
                // console.log(chalk.blue(aclName))
                if (aclNames.indexOf(aclName) < 0)
                    await this.client.systemAclPolicyCreate(aclName, { systemAclPolicyCreateRequest: { contents: aclContents.toString() } });
                else
                    await this.client.systemAclPolicyUpdate(aclName, { systemAclPolicyUpdateRequest: { contents: aclContents.toString() } });
            }
        }
        // console.log('\n\n       Fin        ')
    }
}
exports.ProjectImporter = ProjectImporter;
//# sourceMappingURL=projectImporter.js.map