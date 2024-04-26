import Url from 'url';
import { RundeckInstance } from "./RundeckCluster";
import { DockerCompose } from "./DockerCompose";
export interface IClusterManager {
    startCluster: () => Promise<void>;
    stopCluster: () => Promise<void>;
    stopNode: (node: RundeckInstance) => Promise<void>;
    startNode: (node: RundeckInstance) => Promise<void>;
    listNodes: () => Promise<Url.UrlWithStringQuery[]>;
    logs: () => Promise<void>;
}
interface IConfig {
    licenseFile: string;
    image: string;
    composeFileName?: string;
}
export declare class DockerClusterManager implements IClusterManager {
    readonly dir: string;
    compose: DockerCompose;
    constructor(dir: string, config: IConfig);
    startCluster(): Promise<void>;
    stopCluster(): Promise<void>;
    stopNode(node: RundeckInstance): Promise<void>;
    startNode(node: RundeckInstance): Promise<void>;
    listNodes(): Promise<any[]>;
    logs(): Promise<void>;
}
export declare class ClusterFactory {
    static CreateCluster(dir: string, config: IConfig): Promise<IClusterManager>;
}
export {};
