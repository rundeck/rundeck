interface IConfig {
    env?: {
        [k: string]: string;
    };
    composeFileName: string;
}
export declare class DockerCompose {
    readonly workDir: string;
    readonly config: IConfig;
    constructor(workDir: string, config: IConfig);
    containers(): Promise<String[]>;
    up(service?: string): Promise<void>;
    down(service?: string): Promise<void>;
    stop(service?: string): Promise<void>;
    start(service: string): Promise<void>;
    logs(service?: string): Promise<void>;
}
export {};
