import {BaseCredentialProvider} from 'ts-rundeck/dist/baseCredProvider'
import { parse } from 'url'

import { RundeckCluster, RundeckInstance } from '../RundeckCluster'
import { TestProject, IRequiredResources } from '../TestProject'
import {PasswordCredentialProvider, RundeckClient, rundeckPasswordAuth, TokenCredentialProvider} from 'ts-rundeck'
import { cookieEnrichPolicy, waitForRundeckReady } from '../util/RundeckAPI'
import { ClusterFactory } from '../ClusterManager'

import {envOpts} from './rundeck'

jest.setTimeout(60000)

export interface ITestContext {
    cluster: RundeckCluster
}

export async function CreateRundeckCluster() {
    const rundeckUrl = envOpts.TESTDECK_RUNDECK_URL!

    const clusterManager = await ClusterFactory.CreateCluster(envOpts.TESTDECK_CLUSTER_CONFIG, {
        licenseFile: './license.key',
        image: envOpts.TESTDECK_BASE_IMAGE
    })

    const cluster = new RundeckCluster(rundeckUrl,
      envOpts.TESTDECK_RUNDECK_TOKEN ?
        new RundeckClient(new TokenCredentialProvider(envOpts.TESTDECK_RUNDECK_TOKEN),{baseUri: rundeckUrl}):
        rundeckPasswordAuth('admin','admin', {
            baseUri: rundeckUrl,
        }),
      clusterManager)

    const RundeckNodes = (await clusterManager.listNodes()).filter(u => /rundeck/.test(u.hostname))

    for (let [i, n] of RundeckNodes.entries()) {
        cluster.nodes.push(
            new RundeckInstance(parse(`${n.href}/home/rundeck`),
              clientForBackend(rundeckUrl, envOpts.TESTDECK_RUNDECK_TOKEN ?
                  new TokenCredentialProvider(envOpts.TESTDECK_RUNDECK_TOKEN):
                new PasswordCredentialProvider(rundeckUrl,'admin','admin'),
                `rundeck-${i+1}`))
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

function clientForBackend(url: string, creds:BaseCredentialProvider, backend: string): RundeckClient {
    const cookiePolicy = cookieEnrichPolicy([`backend=${backend}`])

    return new RundeckClient(creds,{
        baseUri: url,
        noRetryPolicy: true,
        requestPolicyFactories: [cookiePolicy]
    })
}