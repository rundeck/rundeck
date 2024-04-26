"use strict";
var __asyncValues = (this && this.__asyncValues) || function (o) {
    if (!Symbol.asyncIterator) throw new TypeError("Symbol.asyncIterator is not defined.");
    var m = o[Symbol.asyncIterator], i;
    return m ? m.call(o) : (o = typeof __values === "function" ? __values(o) : o[Symbol.iterator](), i = {}, verb("next"), verb("throw"), verb("return"), i[Symbol.asyncIterator] = function () { return this; }, i);
    function verb(n) { i[n] = o[n] && function (v) { return new Promise(function (resolve, reject) { v = o[n](v), settle(resolve, reject, v.done, v.value); }); }; }
    function settle(resolve, reject, d, v) { Promise.resolve(v).then(function(v) { resolve({ value: v, done: d }); }, reject); }
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.DockerCompose = void 0;
const child_process_1 = __importDefault(require("child_process"));
const readline_1 = __importDefault(require("readline"));
class DockerCompose {
    constructor(workDir, config) {
        this.workDir = workDir;
        this.config = config;
    }
    async containers() {
        const env = Object.assign(Object.assign({}, process.env), this.config.env || {});
        const cp = child_process_1.default.spawn('docker-compose', ['-f', this.config.composeFileName, 'ps'], { cwd: this.workDir, env });
        const stdout = (async () => {
            var _a, e_1, _b, _c;
            let output = [];
            let burnedHeader = false;
            const rl = readline_1.default.createInterface(cp.stdout);
            try {
                for (var _d = true, rl_1 = __asyncValues(rl), rl_1_1; rl_1_1 = await rl_1.next(), _a = rl_1_1.done, !_a; _d = true) {
                    _c = rl_1_1.value;
                    _d = false;
                    let l = _c;
                    if (burnedHeader) {
                        output.push(l.split(/\s+/)[0]);
                    }
                    else if (l.startsWith('----')) {
                        burnedHeader = true;
                    }
                    else if (l.startsWith('NAME ')) {
                        burnedHeader = true;
                    }
                }
            }
            catch (e_1_1) { e_1 = { error: e_1_1 }; }
            finally {
                try {
                    if (!_d && !_a && (_b = rl_1.return)) await _b.call(rl_1);
                }
                finally { if (e_1) throw e_1.error; }
            }
            return output;
        })();
        return stdout;
    }
    async up(service) {
        const env = Object.assign(Object.assign({}, process.env), this.config.env || {});
        const cp = child_process_1.default.spawn('docker-compose', ['--compatibility', '-f', this.config.composeFileName, 'up', '-d', '--build'], { cwd: this.workDir, stdio: 'inherit', env });
        await new Promise((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code);
                else
                    res();
            });
        });
    }
    async down(service) {
        const env = Object.assign(Object.assign({}, process.env), this.config.env || {});
        const cp = child_process_1.default.spawn('docker-compose', ['--compatibility', '-f', this.config.composeFileName, 'down'], { cwd: this.workDir, stdio: 'inherit', env });
        await new Promise((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code);
                else
                    res();
            });
        });
    }
    async stop(service) {
        const env = Object.assign(Object.assign({}, process.env), this.config.env || {});
        const args = ['-f', this.config.composeFileName, 'stop'];
        if (service)
            args.push(service);
        const cp = child_process_1.default.spawn('docker-compose', args, { cwd: this.workDir, stdio: 'inherit', env });
        await new Promise((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code);
                else
                    res();
            });
        });
    }
    async start(service) {
        const env = Object.assign(Object.assign({}, process.env), this.config.env || {});
        const cp = child_process_1.default.spawn('docker-compose', ['--compatibility', '-f', this.config.composeFileName, 'start', service], { cwd: this.workDir, stdio: 'ignore', env });
        await new Promise((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code);
                else
                    res();
            });
        });
    }
    async logs(service) {
        const env = Object.assign(Object.assign({}, process.env), this.config.env || {});
        const cp = child_process_1.default.spawn('docker-compose', ['--compatibility', '-f', this.config.composeFileName, 'logs'], { cwd: this.workDir, stdio: 'inherit', env });
        await new Promise((res, rej) => {
            cp.on('exit', (code, sig) => {
                if (sig || code != 0)
                    rej(code);
                else
                    res();
            });
        });
    }
}
exports.DockerCompose = DockerCompose;
//# sourceMappingURL=DockerCompose.js.map