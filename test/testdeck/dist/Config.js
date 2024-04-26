"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.Config = void 0;
const fs_1 = require("./async/fs");
const js_yaml_1 = require("js-yaml");
const lodash_1 = __importDefault(require("lodash"));
class Config {
    static async Load(configFile, overrideFile) {
        const config = await this.LoadConfigFile(configFile);
        let override = {};
        try {
            if (overrideFile)
                override = await this.LoadConfigFile(overrideFile);
        }
        catch (e) { }
        return lodash_1.default.merge(config, override);
    }
    static async LoadConfigFile(configFile) {
        const configBytes = await (0, fs_1.readFile)(configFile);
        return (0, js_yaml_1.safeLoad)(configBytes.toString());
    }
}
exports.Config = Config;
//# sourceMappingURL=Config.js.map