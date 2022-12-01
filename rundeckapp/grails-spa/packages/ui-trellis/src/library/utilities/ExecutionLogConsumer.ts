import {ExecutionOutputGetResponse, ExecutionStatusGetResponse, JobWorkflowGetResponse, ExecutionOutput, ExecutionOutputEntry} from '@rundeck/client/dist/lib/models'
import {Rundeck} from '@rundeck/client'

import {RenderedStepList, JobWorkflow} from './JobWorkflow'

type Omit<T, K extends keyof T> = Pick<T, Exclude<keyof T, K>>

export interface IRenderedEntry extends ExecutionOutputEntry {
    renderedStep?: RenderedStepList
    renderedContext?: string
    lineNumber: number
    stepType?: string
}

export type EnrichedExecutionOutput = Omit<ExecutionOutput, 'entries'> & {entries: IRenderedEntry[]}

const BACKOFF_MIN = 100
const BACKOFF_MAX = 5000

export class ExecutionLog {
    client: Rundeck

    offset = 0
    size = 0
    completed = false
    execCompleted = false
    backoff = 0
    lineNumber = 0

    private jobWorkflowProm!: Promise<JobWorkflow>
    private executionStatusProm!: Promise<ExecutionStatusGetResponse>

    constructor(readonly id: string, client?: Rundeck) {
        this.client = client || window._rundeck.rundeckClient
    }

    /** Optional method to populate information about execution output */
    async init() {
        const resp = await this.client.executionOutputGet(this.id, {offset: '0', maxlines: 1})
        this.execCompleted = resp.execCompleted
        this.size = resp.totalSize
    }

    async getJobWorkflow() {   
        if(!this.jobWorkflowProm) {
            this.jobWorkflowProm = (async () => {
                const status = await this.getExecutionStatus()
                if(!status.job){
                    return new JobWorkflow([{exec:status.description,type:'exec',nodeStep:'true'}])
                }
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
        await this.waitBackOff()

        const res = await this.client.executionOutputGet(this.id, {offset: this.offset.toString(), maxlines: maxLines})
        this.offset = parseInt(res.offset)
        this.size = res.totalSize
        this.completed = res.completed && res.execCompleted

        if (!this.completed && res.entries.length == 0) {
            this.increaseBackOff()
        } else {
            this.decreaseBackOff()
        }

        // console.log(`Backoff: ${this.backoff}`)
        // console.log(`Results: ${res.entries.length}`)

        return res
    }

    async waitBackOff() {
        if (this.backoff == 0) {
            return void(0)
        } else {
            return new Promise<void>((res, rej) => {
                setTimeout(res, this.backoff)
            })
        }
    }

    private increaseBackOff() {
        // TODO: Jitter https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/
        this.backoff = Math.min(Math.max(this.backoff, BACKOFF_MIN) * 2, BACKOFF_MAX)
    }

    private decreaseBackOff() {
        if (this.backoff == 0)
            return
    
        const backoff = this.backoff / 2
        this.backoff = backoff < BACKOFF_MIN ? 0 : backoff
    }

    async getEnrichedOutput(maxLines: number): Promise<EnrichedExecutionOutput> {
        const [workflow, res] = await Promise.all([
            this.getJobWorkflow(),
            this.getOutput(maxLines)
        ])
        
        const enrichedEntries = res.entries.map(e => {
            this.lineNumber++
            return {
                lineNumber: this.lineNumber,
                renderedStep: e.stepctx ? workflow.renderStepsFromContextPath(e.stepctx!) : undefined,
                renderedContext: e.stepctx ? workflow.renderContextString(e.stepctx!) : undefined,
                stepType: e.stepctx ? workflow.contextType(e.stepctx!) : undefined,
                ...e
            }
        })

        return {
            ...res,
            entries: enrichedEntries
        }
    }
}