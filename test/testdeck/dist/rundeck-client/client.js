"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.Client = void 0;
const axios_1 = __importDefault(require("axios"));
class Client {
    constructor(opts) {
        this.opts = opts;
        this.c = axios_1.default.create({
            headers: {
                'Content-Type': 'application/json',
            },
            maxRedirects: 0,
        });
    }
    login() {
        if (!this.loginProm)
            this.loginProm = this._doLogin();
        return this.loginProm;
    }
    async listUsers() {
        const path = `${this.opts.apiUrl}/api/21/user/list`;
        return this.get(path);
    }
    async systemInfo() {
        const path = `${this.opts.apiUrl}/api/14/system/info`;
        return this.get(path);
    }
    async get(url, config) {
        await this.login();
        return await this.c.get(url, config);
    }
    async _doLogin() {
        try {
            const { apiUrl, username, password } = this.opts;
            const path = `${apiUrl}/j_security_check?j_username=${username}&j_password=${password}`;
            const resp = await this.c.post(path, null, { validateStatus: s => s >= 300 && s <= 400 });
            this.c.defaults.headers.Cookie = resp.headers['set-cookie'][0];
        }
        catch (e) {
            this.loginProm = undefined;
            throw e;
        }
    }
}
exports.Client = Client;
//# sourceMappingURL=client.js.map