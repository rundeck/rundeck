import CP from 'child_process'
import FS from 'fs'

import {promisify} from 'util'

import URL from 'url'
import { RundeckClient, rundeckPasswordAuth } from 'ts-rundeck'
import { IClusterManager } from './ClusterManager'

const readfileAsync = promisify(FS.readFile)

export class RundeckCluster {
    url: URL

    client: RundeckClient

    nodes: RundeckInstance[] = []

    clusterManager: IClusterManager

    constructor(url: string, client:RundeckClient, clusterManager: IClusterManager) {
        this.client = client
        this.clusterManager = clusterManager
    }

    /** Write a file to Rundeck base directory on all nodes in the cluster */
    async writeRundeckFile(file: string, data: Buffer) {
        return await Promise.all(
            this.nodes.map(n => n.writeRundeckFile(file, data))
        )
    }

    /** Write a file to all nodes in the cluster */
    async writeFile(file: string, data: Buffer) {
        return await Promise.all(
            this.nodes.map(n => n.writeFile(file, data))
        )
    }

    async stopNode(node:RundeckInstance) {
        return await this.clusterManager.stopNode(node)
    }

    async startNode(node:RundeckInstance) {
        return await this.clusterManager.startNode(node)
    }
}

export class RundeckInstance {
    constructor(readonly base: URL.UrlWithStringQuery, readonly client: RundeckClient)  {}

    async readRundeckFile(file: string) {
        const readUrl = `${this.base.href}/${file}`
        return await readFileUrl(URL.parse(readUrl))
    }

    async readFile(file: string) {
        const {base} = this
        const readUrl = `${base.protocol}//${base.host}/${file}`
        return await readFileUrl(URL.parse(readUrl))
    }

    async writeRundeckFile(file: string, data: Buffer) {
        const {base} = this
        const writeUrl = `${base.href}/${file}`
        return await writeFileUrl(URL.parse(writeUrl), data)
    }

    async writeFile(file: string, data: Buffer) {
        const {base} = this
        const writeUrl = `${base.protocol}//${base.host}/${file}`
        return await writeFileUrl(URL.parse(writeUrl), data)
    }
}

function readFileUrl(url: URL.UrlWithStringQuery): Promise<Buffer> {
    switch(url.protocol) {
        case('docker:'):
            return readDockerFile(url)
        case('file:'):
            return readLocalFile(url)
        default:
            throw new Error(`Unsupported protocol ${url.protocol}`)
    }
}

function writeFileUrl(url: URL.UrlWithStringQuery, data: Buffer): Promise<void> {
    switch(url.protocol) {
        case('docker:'):
            return writeDockerFile(url, data)
        default:
            throw new Error(`Unsupported protocol ${url.protocol}`)
    }
}

async function readLocalFile(url: URL.UrlWithStringQuery): Promise<Buffer> {
    return readfileAsync(url.pathname)
}

async function readDockerFile(url: URL.UrlWithStringQuery): Promise<Buffer> {
    const cp = CP.spawn('docker', ['exec', url.host, 'cat', url.pathname])
    return new Promise((res, rej) => {
        const output = [] as Buffer[]
        const error = [] as Buffer[]

        cp.stdout.on('data', (chunk: Buffer) => {
            output.push(chunk)
        })

        cp.stderr.on('data', (chunk: Buffer) => {
            error.push(chunk)
        })

        cp.on('exit', (code, sig) => {
            if (code != 0)
                rej(new Error(`Error reading file ${url}\n\n${Buffer.concat(error).toString()}`))
            else
                res(Buffer.concat(output))
        })
    })
}

async function writeDockerFile(url: URL.UrlWithStringQuery, data: Buffer): Promise<void> {
    const cp = CP.spawn('docker', ['exec', '-i', url.host, 'bash', '-c', `cat > ${url.pathname}`])
    return new Promise((res, rej) => {
        const output = [] as Buffer[]
        const error = [] as Buffer[]

        cp.stdout.on('data', (chunk: Buffer) => {
            output.push(chunk)
        })

        cp.stderr.on('data', (chunk: Buffer) => {
            error.push(chunk)
        })

        cp.on('exit', (code, sig) => {
            if (code != 0)
                rej(new Error(`Error writing file ${URL.format(url)}\n\n${Buffer.concat(error).toString()}`))
            else
                res()
        })

        cp.stdin.write(data)
        cp.stdin.end()
    })
}
