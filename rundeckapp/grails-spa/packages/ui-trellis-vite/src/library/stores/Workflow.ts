import {observable, action, runInAction} from 'mobx'

import {RundeckClient} from '@rundeck/client'
import { RootStore } from './RootStore'
import { JobWorkflow } from '../utilities/JobWorkflow'

export class WorkflowStore {
    @observable.shallow workflows: Map<string, JobWorkflow> = new Map()

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    @action
    async get(jobId: string) {
        if (!this.workflows.has(jobId)) {
            const workflow = await this.fetch(jobId)
            runInAction( () => {
                this.workflows.set(jobId, workflow)
            })
        }
        return this.workflows.get(jobId)!
    }

    private async fetch(jobId: string) {
        const resp = await this.client.jobWorkflowGet(jobId)
        return new JobWorkflow(resp.workflow)
    }
}

class WorkflowStep {
    jobref?: WorkflowStepJobref
    jobId?: string
    description?: string
    exec?: string
    script?: string
    scriptfile?: string
    scripturl?: string
    type?: string
    nodeStep?: string
    workflow?: WorkflowStep[]

}

class WorkflowStepJobref {
    name?: string;
    group?: string;
    uuid?: string;
    nodeStep?: string;
    importOptions?: boolean;
}