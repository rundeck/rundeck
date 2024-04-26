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
exports.EnvBuilder = void 0;
const util_1 = require("./async/util");
const CP = __importStar(require("./async/child-process"));
/**
 * Consolidates environment setup and teardown and exposes through
 * two primary methods: up and down.
 */
class EnvBuilder {
    constructor(client) {
        this.client = client;
    }
    /**
     * Returns after the testing environment is ready.
     */
    async up() {
        console.log(`
Setting up test environment.
This can take a few minutes if the docker conatiner is not running.
Sit tight...\n
        `);
        await CP.exec('docker-compose up -d');
        await this.waitForRundeckReady();
    }
    /**
     * Returns after the testing environment has been cleaned up.
     */
    async down() {
        console.log(`
Tearing down test environment...
        `);
        await CP.exec('docker-compose down');
    }
    /**
     * Continually checks for Rundeck readyness by attempting to login.
     * Throws an error login is not successful within the timeout period.
     */
    async waitForRundeckReady(timeout = 120000) {
        const start = Date.now();
        while (Date.now() - start < timeout) {
            try {
                await this.client.login();
                return;
            }
            catch (e) {
                await (0, util_1.sleep)(5000);
            }
        }
        throw new Error('Timeout exceeded waiting for Rundeck to be ready.');
    }
}
exports.EnvBuilder = EnvBuilder;
//# sourceMappingURL=env-builder.js.map