import {ParseBool} from '../util/parseBool'
import { RundeckCluster, RundeckInstance } from '../RundeckCluster'
import { parse } from 'url'
import { TestProject, IRequiredResources } from '../TestProject'
import { Rundeck, rundeckPasswordAuth } from 'ts-rundeck'
import { cookieEnrichPolicy, waitForRundeckReady } from '../util/RundeckAPI'
import { DockerClusterManager, ClusterFactory } from '../ClusterManager'

jest.setTimeout(60000)

export interface ITestContext {
    cluster: RundeckCluster
}

export const envOpts = {
    RUNDECK_URL: process.env.RUNDECK_URL || 'http://127.0.0.1:4440',
    CI: ParseBool(process.env.CI),
    HEADLESS: ParseBool(process.env.HEADLESS) || ParseBool(process.env.CI),
    S3_UPLOAD: ParseBool(process.env.S3_UPLOAD) || ParseBool(process.env.CI),
    S3_BASE: process.env.S3_BASE,
    TESTDECK_CLUSTER_CONFIG: process.env.TESTDECK_CLUSTER_CONFIG
}

export async function CreateRundeckCluster() {
    const rundeckUrl = envOpts.RUNDECK_URL!

    const clusterManager = await ClusterFactory.CreateCluster(envOpts.TESTDECK_CLUSTER_CONFIG, {
        licenseFile: './license.key',
        image: 'rundeckpro/enterprise:SNAPSHOT'
    })

    const cluster = new RundeckCluster(envOpts.RUNDECK_URL!, 'admin', 'admin', clusterManager)

    const RundeckNodes = (await clusterManager.listNodes()).filter(u => /rundeck/.test(u.hostname))

    for (let [i, n] of RundeckNodes.entries()) {
        cluster.nodes.push(
            new RundeckInstance(parse(`${n.href}/home/rundeck`), clientForBackend(rundeckUrl, `rundeck-${i+1}`))
        )
    }

    return cluster
}

export function CreateTestContext(resources: IRequiredResources) {
    let context = {cluster: null}

    beforeAll( async () => {
        context.cluster = await CreateRundeckCluster()
        await waitForRundeckReady(context.cluster.client)
        await TestProject.LoadResources(context.cluster.client, resources)
    })

    return context as ITestContext
}

function clientForBackend(url: string, backend: string): Rundeck {
    const cookiePolicy = cookieEnrichPolicy([`backend=${backend}`])

    return rundeckPasswordAuth('admin', 'admin', {
        baseUri: url,
        noRetryPolicy: true,
        requestPolicyFactories: [cookiePolicy]
    })
}