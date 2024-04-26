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
exports.TestRepo = void 0;
const Path = __importStar(require("path"));
const FS = __importStar(require("./async/fs"));
/**
 * Represents the discovered test folder.
 */
class TestRepo {
    /** Returns a TestRepo constructed from the supplied path. */
    static async CreateTestRepo(path, filter) {
        const absPath = Path.resolve(path);
        const groups = await this._loadRepo(absPath, filter);
        return new TestRepo(groups);
    }
    static async _loadRepo(path, filter) {
        const dirContents = await this._dirContents(path);
        const dirStats = await this._statFiles(dirContents);
        const dirs = await dirStats.filter(e => e.stats.isDirectory());
        const groupProms = dirs.map(d => this._loadRepoFolder(d.file, filter));
        groupProms.push(this._loadRepoFolder(path, filter, 'main'));
        return await Promise.all(groupProms);
    }
    static async _loadRepoFolder(path, filter, groupName) {
        groupName = groupName ? groupName : path.split(Path.sep).pop();
        const dirContents = (await FS.readdir(path)).map(f => Path.join(path, f));
        const dirStats = (await Promise.all(dirContents.map(f => FS.stat(f)))).map((s, i) => ({ file: dirContents[i], stats: s }));
        const testEntries = dirStats.filter(e => e.stats.isFile() && filter.test(e.file));
        const tests = testEntries.map(t => ({
            file: t.file,
            name: Path.basename(t.file).split('.').shift(),
        }));
        return {
            name: groupName,
            tests,
        };
    }
    static async _dirContents(path) {
        return (await FS.readdir(path)).map(f => Path.join(path, f));
    }
    static async _statFiles(paths) {
        return (await Promise.all(paths.map(f => FS.stat(f)))).map((s, i) => ({ file: paths[i], stats: s }));
    }
    constructor(groups) {
        this.groups = groups;
    }
}
exports.TestRepo = TestRepo;
//# sourceMappingURL=test-repo.js.map