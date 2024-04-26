export interface ITestDeckConfig {
    baseImage: string;
    licenseFile: string;
    clusterConfig: string;
    url: string;
}
export declare class Config {
    static Load(configFile: string, overrideFile?: string): Promise<ITestDeckConfig>;
    private static LoadConfigFile;
}
