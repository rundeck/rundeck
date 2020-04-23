import { readFile } from "./async/fs";

import {safeLoad} from 'js-yaml'

export interface ITestDeckConfig {
    baseImage: string
    licenseFile: string
    clusterConfig: string
}

export class Config {
    static async Load(configFile: string): Promise<ITestDeckConfig> {
        const configBytes = await readFile(configFile)

        return  safeLoad(configBytes.toString())
    }
}