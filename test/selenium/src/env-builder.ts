import {sleep} from './async/util'

import * as CP from './async/child-process'
import {Client} from './rundeck-client/client'

/**
 * Consolidates environment setup and teardown and exposes through
 * two primary methods: up and down.
 */
export class EnvBuilder {
    constructor(readonly client: Client) {}

    /**
     * Returns after the testing environment is ready.
     */
    async up() {
        console.log(`
Setting up test environment.
This can take a few minutes if the docker conatiner is not running.
Sit tight...\n
        `)

        await CP.exec('docker-compose up -d')
        await this.waitForRundeckReady()
    }

    /**
     * Returns after the testing environment has been cleaned up.
     */
    async down() {
        console.log(`
Tearing down test environment...
        `)

        await CP.exec('docker-compose down')
    }

    /**
     * Continually checks for Rundeck readyness by attempting to login.
     * Throws an error login is not successful within the timeout period.
     */
    async waitForRundeckReady(timeout = 120000) {
        const start = Date.now()
        while (Date.now() - start < timeout) {
            try {
                await this.client.login()
                return
            } catch  (e) {
                await sleep(5000)
            }
        }
        throw new Error('Timeout exceeded waiting for Rundeck to be ready.')
    }
}