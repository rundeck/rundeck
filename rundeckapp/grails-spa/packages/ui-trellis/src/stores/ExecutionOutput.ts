import {observable, action, IObservableArray} from 'mobx'

import {RundeckClient} from 'ts-rundeck'
import { RootStore } from './RootStore'
import { JobWorkflow, RenderedStepList } from '../utilities/JobWorkflow'
import { ExecutionStatusGetResponse, ExecutionOutputGetResponse, ExecutionOutputEntry as ApiExecutionOutputEntry } from 'ts-rundeck/dist/lib/models'

// export type EnrichedExecutionOutput = Omit<ExecutionOutput, 'entries'> & {entries: IRenderedEntry[]}

const BACKOFF_MIN = 100
const BACKOFF_MAX = 5000

export class ExecutionOutputStore {
    @observable executionOutputsById: Map<string, ExecutionOutput> = new Map()

    constructor(readonly root: RootStore,readonly client: RundeckClient) {}

    @action
    createOrGet(id: string) {
        if (!this.executionOutputsById.has(id))
            this.executionOutputsById.set(id, new ExecutionOutput(id, this.client))

        return this.executionOutputsById.get(id)!
    }

    async getOutput(id: string, maxLines: number ) {
        const output = this.createOrGet(id)

        await output.getOutput(maxLines)
        return output
    }

}

export class ExecutionOutput {
    client!: RundeckClient

    id!: string
    offset: number = 0
    clusterExec!: boolean

    @observable completed!: boolean
    @observable execCompleted!: boolean
    @observable hasFailedNodes!: boolean
    @observable execState!: string
    @observable lastModified!: string
    @observable execDuration!: number
    @observable percentLoaded!: number
    @observable totalSize!: number
    @observable retryBackoff!: number

    compacted!: boolean

    entries: IObservableArray<ExecutionOutputEntry> = observable.array([])

    @observable.shallow entriesByNode: Map<string, ExecutionOutputEntry[]> = new Map()

    constructor(id: string, client: RundeckClient) {
        Object.assign(this, { id, client })
    }

    size = 0
    backoff = 0
    lineNumber = 0

    private jobWorkflowProm!: Promise<JobWorkflow>
    private executionStatusProm!: Promise<ExecutionStatusGetResponse>

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

    async getOutput(maxLines: number): Promise<ExecutionOutputEntry[]> {
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

        const newEntries: ExecutionOutputEntry[] = []
        for (const entry of res.entries) {
            this.lineNumber++
            const entryObj = ExecutionOutputEntry.FromApiResponse(this, entry, this.lineNumber)
            newEntries.push(entryObj)
        }
        this.entries.push(...newEntries)
        return newEntries
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

    // async getEnrichedOutput(maxLines: number): Promise<EnrichedExecutionOutput> {
    //     const [workflow, res] = await Promise.all([
    //         this.getJobWorkflow(),
    //         this.getOutput(maxLines)
    //     ])
        
    //     const enrichedEntries = res.entries.map(e => {
    //         this.lineNumber++
    //         return {
    //             lineNumber: this.lineNumber,
    //             renderedStep: e.stepctx ? workflow.renderStepsFromContextPath(e.stepctx!) : undefined,
    //             renderedContext: e.stepctx ? workflow.renderContextString(e.stepctx!) : undefined,
    //             stepType: e.stepctx ? workflow.contextType(e.stepctx!) : undefined,
    //             ...e
    //         }
    //     })

    //     return {
    //         ...res,
    //         entries: enrichedEntries
    //     }
    // }
}

export class ExecutionOutputEntry {
    executionOutput: ExecutionOutput

    time!: string
    absoluteTime!: string
    log?: string
    logHtml?: string
    level?: string
    stepctx?: string
    node?: string
    lineNumber!: number

    renderedStep?: RenderedStepList

    constructor(executionOutput: ExecutionOutput) {
        this.executionOutput = executionOutput
    }

    static FromApiResponse(executionOutput: ExecutionOutput, resp: ApiExecutionOutputEntry, line: number) {
        const entry = new ExecutionOutputEntry(executionOutput)
        entry.time = resp.time!
        entry.absoluteTime = resp.absoluteTime!
        entry.log = resp.log
        entry.level = resp.level
        // @ts-ignore
        entry.logHtml = resp.logHtml
        entry.stepctx = resp.stepctx
        entry.node = resp.node
        entry.lineNumber = line

        return entry
    }

    // label = this.entryStepLabel(newEntry)
    // stepType = this.entryStepType(newEntry)
    // path = this.entryPath(newEntry)


    // private entryStepType(newEntry: IRenderedEntry) {
    //     if (!newEntry.renderedStep)
    //       return ''
    
    //     const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
    //     return lastStep ? lastStep.type : ''
    //   }
    
    //   private entryStepLabel(newEntry: IRenderedEntry) {
    //     if (!newEntry.renderedStep)
    //       return ''
    
    //     const lastStep = newEntry.renderedStep[newEntry.renderedStep.length -1]
    //     const label = lastStep ? 
    //       `${lastStep.stepNumber}${lastStep.label}` :
    //       newEntry.stepctx
    
    //     return label
    //   }
    
    //   private entryPath(newEntry: IRenderedEntry) {
    //     if (!newEntry.renderedStep)
    //       return ''
    
    //     let stepString = newEntry.renderedStep.map( s => {
    //       if (!s)
    //         return '..'
    //       return `${s.stepNumber}${s.label}`
    //     })
    //     return stepString.join(' / ')
    //   }
    
    //   private entryTitle(newEntry: IRenderedEntry) {
    //     return `#${newEntry.lineNumber} ${newEntry.absoluteTime} ${this.entryPath(newEntry)}`
    //   }
}