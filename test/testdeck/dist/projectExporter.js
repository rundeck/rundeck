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
exports.ProjectExporter = exports.KeyType = void 0;
const Path = __importStar(require("path"));
const util = __importStar(require("util"));
const chalk_1 = __importDefault(require("chalk"));
const mkdirp_1 = __importDefault(require("mkdirp"));
const js_yaml_1 = __importDefault(require("js-yaml"));
const child_process_1 = require("./async/child-process");
const Tmp = __importStar(require("./async/tmp"));
const FS = __importStar(require("./async/fs"));
const stream_1 = require("./async/stream");
const MakeDirAsync = util.promisify(mkdirp_1.default);
var KeyType;
(function (KeyType) {
    KeyType["Public"] = "publicKey";
    KeyType["Private"] = "privateKey";
    KeyType["Password"] = "password";
})(KeyType || (exports.KeyType = KeyType = {}));
class ProjectExporter {
    constructor(repoPath, projectName, client) {
        this.repoPath = repoPath;
        this.projectName = projectName;
        this.client = client;
    }
    async exportProject() {
        const dir = await Tmp.dir({ prefix: 'demo_projects_' });
        const exportFileName = Path.join(dir, this.projectName);
        const projectDir = Path.join(this.repoPath, this.projectName);
        /**
         * Project export
         */
        console.log('Exporting project...');
        console.log(chalk_1.default.blue(this.projectName));
        const exportResp = await this.client.projectArchiveExportSync(this.projectName, { exportExecutions: false });
        let projectArchive;
        if (exportResp.readableStreamBody)
            projectArchive = await (0, stream_1.readStream)(exportResp.readableStreamBody);
        else
            throw new Error('Stream not readable');
        await FS.writeFile(exportFileName, projectArchive);
        console.log('Exploding export into git repo...');
        // Clear out existing exported data so we sync removed resources
        await (0, child_process_1.exec)(`rm -r ${projectDir}/rundeck-* || true`);
        await (0, child_process_1.exec)(`unzip -oq ${exportFileName} -d ${projectDir}/ -x '*/reports/*' -x '*/executions/*'`);
        /**
         * Public key export
         */
        const keyList = await this.client.storageKeyGetMetadata(this.projectName);
        if (keyList._response.status == 200) {
            const keys = keyList.resources
                .filter(k => k.meta.rundeckKeyType == 'public')
                .map(k => {
                return { path: k.path, type: k.meta.rundeckKeyType, value: '' };
            });
            for (let key of keys) {
                const keyPath = key.path.split('/').slice(1).join('/');
                const resp = await this.client.storageKeyGetMaterial(keyPath, { customHeaders: { accept: '*/*' } });
                if (resp.readableStreamBody)
                    key.value = (await (0, stream_1.readStream)(resp.readableStreamBody)).toString();
                else
                    throw new Error('Stream not readable');
                const keyRelPath = keyPath.split('/').slice(1).join();
                const KeyRepoPath = Path.join(projectDir, 'system/keys', `${keyRelPath}.yaml`);
                await MakeDirAsync(Path.dirname(KeyRepoPath));
                await FS.writeFile(KeyRepoPath, js_yaml_1.default.dump(key));
            }
        }
        /**
         * System ACL Export
         */
        const aclRepoPath = Path.join(projectDir, 'system/acls');
        await MakeDirAsync(aclRepoPath);
        const acls = await this.client.systemAclPolicyList();
        const projectAcls = acls.resources
            .filter(a => a.name.toLowerCase().startsWith(this.projectName.toLowerCase()));
        for (const acl of projectAcls) {
            console.log(chalk_1.default.blue(acl.name));
            const aclRepoFile = Path.join(aclRepoPath, `${acl.name}.yaml`);
            const aclResp = await this.client.systemAclPolicyGet(acl.name);
            const aclContent = aclResp.contents;
            await FS.writeFile(aclRepoFile, aclContent);
        }
        // console.log('\n\n       Fin        ')
    }
}
exports.ProjectExporter = ProjectExporter;
//# sourceMappingURL=projectExporter.js.map