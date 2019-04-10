import * as Url from 'url'

import {S3} from 'aws-sdk'

import {WebDriver} from 'selenium-webdriver'
import { generateUuid } from 'ms-rest-js';

export class Context {
    currentTestName!: string
    s3: S3
    uploadPromises: Promise<{}>[] = []
    snapCounter = 0
    contextId: string

    constructor(readonly driver: WebDriver, readonly baseUrl: string, readonly s3Upload: boolean, readonly s3Base: string) {
        this.s3 = new S3({region: 'us-west-2'})
        this.contextId = generateUuid().slice(0,4)
    }

    urlFor(path: string) {
        return Url.resolve(this.baseUrl, path)
    }

    friendlyTestName() {
        return this.currentTestName.toLowerCase().replace(/ /g, '_')
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