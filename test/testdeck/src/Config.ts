import { readFile } from "./async/fs"

import {promises as FS} from 'fs'

import {safeLoad} from 'js-yaml'

import _ from 'lodash'

export interface ITestDeckConfig {
    baseImage: string
    licenseFile: string
    clusterConfig: string
    url: string
}

export class Config {
    static async Load(configFile: string, overrideFile?: string): Promise<ITestDeckConfig> {
        const config = await this.LoadConfigFile(configFile)
        let override = {} as Partial<ITestDeckConfig>
        try {
            if (overrideFile)
                override = await this.LoadConfigFile(overrideFile)
        } catch(e) {}

        return _.merge(config, override)
    }

    private static async LoadConfigFile(configFile: string): Promise<ITestDeckConfig> {
        const configBytes = await readFile(configFile)
        return safeLoad(configBytes.toString()) 
    }
}