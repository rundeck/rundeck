import {ExecutionOutputGetResponse, ExecutionStatusGetResponse, JobWorkflowGetResponse} from 'ts-rundeck/dist/lib/models'
import {Rundeck, TokenCredentialProvider} from 'ts-rundeck'

export class ExecutionLog {
    client: Rundeck

    offset = '0'
    completed = false

    private jobWorkflowProm!: Promise<JobWorkflowGetResponse>
    private executionStatusProm!: Promise<ExecutionStatusGetResponse>

    constructor(readonly id: string) {
        // For testing outside app
        // this.client = new Rundeck(new TokenCredentialProvider(''), {baseUri: 'http://ubuntu:4440'})
        this.client = window._rundeck.rundeckClient
    }

    async getJobWorkflow() {
        if(!this.jobWorkflowProm) {
            const status = await this.getExecutionStatus()
            this.jobWorkflowProm = this.client.jobWorkflowGet(status.job!.id!)
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
}