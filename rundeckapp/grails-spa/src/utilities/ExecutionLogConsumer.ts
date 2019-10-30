import {ExecutionOutputGetResponse, ExecutionStatusGetResponse, JobWorkflowGetResponse, ExecutionOutput, ExecutionOutputEntry} from 'ts-rundeck/dist/lib/models'
import {Rundeck, TokenCredentialProvider} from 'ts-rundeck'

import {IRenderedStep, RenderedStepList, JobWorkflow} from './JobWorkflow'

type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>

interface IRenderedEntry extends ExecutionOutputEntry {
    renderedStep: RenderedStepList
}

export type EnrichedExecutionOutput = Omit<ExecutionOutput, 'entries'> & {entries: IRenderedEntry[]}

export class ExecutionLog {
    client: Rundeck

    offset = '0'
    completed = false

    private jobWorkflowProm!: Promise<JobWorkflow>
    private executionStatusProm!: Promise<ExecutionStatusGetResponse>

    constructor(readonly id: string) {
        // For testing outside app
        // this.client = new Rundeck(new TokenCredentialProvider(''), {baseUri: 'http://ubuntu:4440'})
        this.client = window._rundeck.rundeckClient
    }

    async getJobWorkflow() {   
        if(!this.jobWorkflowProm) {
            this.jobWorkflowProm = (async () => {
                const status = await this.getExecutionStatus()
                let resp = await this.client.jobWorkflowGet(status.job!.id!)
                return new JobWorkflow(resp.workflow)
            })()
        }
        return this.jobWorkflowProm
    }

    async getExecutionStatus() {
        if (!this.executionStatusProm)
            this.executionStatusProm = this.client.executionStatusGet(this.id)
        
        return this.executionStatusProm
    }

    async getOutput(maxLines: number): Promise<ExecutionOutputGetResponse> {
        const res = await this.client.executionOutputGet(this.id, {offset: this.offset, maxlines: maxLines})
        this.offset = res.offset
        this.completed = res.completed
        return res
    }

    async getEnrichedOutput(maxLines: number): Promise<EnrichedExecutionOutput> {
        const [workflow, res] = await Promise.all([
            this.getJobWorkflow(),
            this.client.executionOutputGet(this.id, {offset: this.offset, maxlines: maxLines})
        ])
        this.offset = res.offset
        this.completed = res.completed

        const enrichedEntries = res.entries.map(e => ({
            renderedStep: workflow.renderStepsFromContextPath(e.stepctx!),
            ...e
        }))

        return {
            ...res,
            entries: enrichedEntries
        }
    }
}