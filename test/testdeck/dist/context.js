"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.Context = void 0;
const fs_1 = __importDefault(require("fs"));
const util_1 = require("util");
const aws_sdk_1 = require("aws-sdk");
const uuid_1 = require("uuid");
const writeAsync = (0, util_1.promisify)(fs_1.default.writeFile);
class Context {
    constructor(driverProvider, baseUrl, s3Upload, s3Base) {
        this.driverProvider = driverProvider;
        this.baseUrl = baseUrl;
        this.s3Upload = s3Upload;
        this.s3Base = s3Base;
        this.uploadPromises = [];
        this.snapCounter = 0;
        this.s3 = new aws_sdk_1.S3({ region: 'us-west-2' });
        this.contextId = (0, uuid_1.v1)().slice(0, 4);
    }
    async init() {
        this.driver = await this.driverProvider();
    }
    urlFor(path) {
        return `${this.baseUrl.endsWith('/') ? this.baseUrl.substring(0, this.baseUrl.length - 1) : this.baseUrl}/${path.startsWith('/') ? path.substring(1) : path}`;
    }
    friendlyTestName() {
        return this.currentTestName.toLowerCase().replace(/ /g, '_');
    }
    async screenshot() {
        return await this.driver.takeScreenshot();
    }
    async dispose() {
        await this.driver.close();
        await Promise.all(this.uploadPromises);
    }
    async screenSnap(name) {
        const snapFileName = `${this.contextId}-${this.snapCounter}-${this.friendlyTestName()}-${name}.png`;
        /** Import to increment counter before async calls */
        this.snapCounter++;
        const screen = await this.screenshot();
        await writeAsync(`test_out/images/${snapFileName}`, new Buffer(screen, 'base64'));
        if (this.s3Upload)
            await this.screenCapToS3(screen, snapFileName);
        return screen;
    }
    async screenCapToS3(screen, name) {
        this.uploadPromises.push(this.s3.putObject({ Bucket: 'test.rundeck.org', Key: `${this.s3Base}/${name}`, Body: Buffer.from(screen, 'base64'), ContentType: 'image/png' }).promise());
    }
}
exports.Context = Context;
//# sourceMappingURL=context.js.map