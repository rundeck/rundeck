import FS from 'fs'
import * as Url from 'url'
import {promisify} from 'util'
import {RundeckCluster} from './RundeckCluster'

import {S3} from 'aws-sdk'

import {WebDriver} from 'selenium-webdriver'

import {v1} from 'uuid'

const writeAsync = promisify(FS.writeFile)

export class Context {
    Rundeck: RundeckCluster
    currentTestName!: string
    s3: S3
    uploadPromises: Promise<{}>[] = []
    snapCounter = 0
    contextId: string
    driver!: WebDriver

    constructor(readonly driverProvider: () => Promise<WebDriver>, readonly baseUrl: string, readonly s3Upload: boolean, readonly s3Base: string) {
        this.s3 = new S3({region: 'us-west-2'})
        this.contextId = v1().slice(0,4)
    }

    async init() {
        this.driver = await this.driverProvider()
    }

    urlFor(path: string) {
        return `${this.baseUrl.endsWith('/') ? this.baseUrl.substring(0, this.baseUrl.length - 1) : this.baseUrl}/${path.startsWith('/') ? path.substring(1) : path}`
    }

    friendlyTestName() {
        return this.currentTestName.toLowerCase().replace(/ /g, '_');
    }

    async screenshot() {
        return await this.driver.takeScreenshot()
    }

    async dispose() {
        await this.driver.close()
        await Promise.all(this.uploadPromises)
    }

    async screenSnap(name: string) {
        const snapFileName = `${this.contextId}-${this.snapCounter}-${this.friendlyTestName()}-${name}.png`

        /** Import to increment counter before async calls */
        this.snapCounter++

        const screen = await this.screenshot()

        await writeAsync(`test_out/images/${snapFileName}`, new Buffer(screen, 'base64'))

        if (this.s3Upload)
            await this.screenCapToS3(screen, snapFileName)

        return screen
    }

    async screenCapToS3(screen: string, name: string) {
        this.uploadPromises.push(
            this.s3.putObject(
                {Bucket: 'test.rundeck.org', Key: `${this.s3Base}/${name}`, Body: Buffer.from(screen, 'base64'), ContentType: 'image/png'}
            ).promise()
        )
    }
}
