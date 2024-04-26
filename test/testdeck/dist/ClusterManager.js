"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ClusterFactory = exports.DockerClusterManager = void 0;
const path_1 = __importDefault(require("path"));
const url_1 = __importDefault(require("url"));
const DockerCompose_1 = require("./DockerCompose");
const DEFAULT_DOCKER_COMPOSE_FILE_NAME = 'docker-compose.yml';
class DockerClusterManager {
    constructor(dir, config) {
        this.dir = dir;
        this.compose = new DockerCompose_1.DockerCompose(dir, {
            env: {
                RUNDECK_LICENSE_FILE: path_1.default.resolve(config.licenseFile),
                RUNDECK_IMAGE: config.image,
                COMPOSE_PROJECT_NAME: 'testdeck'
            },
            composeFileName: config.composeFileName || DEFAULT_DOCKER_COMPOSE_FILE_NAME
        });
    }
    async startCluster() {
        await this.compose.up();
    }
    async stopCluster() {
        await this.compose.stop();
    }
    async stopNode(node) {
        const { base } = node;
        if (base.protocol != 'docker:')
            throw new Error(`Protocol not supported: ${base.protocol}`);
        const serviceName = base.hostname.split('_')[1];
        await this.compose.stop(serviceName);
    }
    async startNode(node) {
        const { base } = node;
        if (base.protocol != 'docker:')
            throw new Error(`Protocol not supported: ${base.protocol}`);
        const serviceName = base.hostname.split('_')[1];
        await this.compose.start(serviceName);
    }
    async listNodes() {
        const containers = await this.compose.containers();
        return containers.map(c => url_1.default.parse(`docker://${c}`));
    }
    async logs() {
        await this.compose.logs();
    }
}
exports.DockerClusterManager = DockerClusterManager;
class ClusterFactory {
    static async CreateCluster(dir, config) {
        // TODO: Support non-docker clusters
        console.log(`Creating cluster from ${path_1.default.resolve(dir)}`);
        return new DockerClusterManager(dir, config);
    }
}
exports.ClusterFactory = ClusterFactory;
//# sourceMappingURL=ClusterManager.js.map