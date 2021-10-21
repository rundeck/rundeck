import {observable, action, IObservableArray, observe, IArrayChange, IArraySplice} from 'mobx'
import {ObservableGroupMap, actionAsync, task} from 'mobx-utils'

import {RundeckClient} from '@rundeck/client'
import { RootStore } from './RootStore'
import { JobWorkflow, RenderedStepList } from '../utilities/JobWorkflow'
import { ExecutionStatusGetResponse, ExecutionOutputGetResponse, ExecutionOutputEntry as ApiExecutionOutputEntry } from '@rundeck/client/dist/lib/models'
import { Serial } from '../utilities/Async'

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

    @observable error!: string
    @observable empty: boolean = false
    @observable completed!: boolean
    @observable execCompleted!: boolean
    @observable hasFailedNodes!: boolean
    @observable execState!: string
    @observable lastModified!: string
    @observable execDuration!: number
    @observable percentLoaded: number = 0
    @observable totalSize!: number
    @observable retryBackoff!: number

    compacted!: boolean

    entries: IObservableArray<ExecutionOutputEntry> = observable.array([], {deep: false})

    entriesbyNodeCtx: ObservableGroupMap<string, ExecutionOutputEntry>

    @observable.shallow entriesByNode: ObservableGroupMap<string, ExecutionOutputEntry>

    constructor(id: string, client: RundeckClient) {
        Object.assign(this, { id, client })
        this.entriesbyNodeCtx = new ObservableGroupMap(this.entries, (e) => {
            return `${e.node}:${e.stepctx ? JobWorkflow.cleanContextId(e.stepctx) : ''}`
        })
        this.entriesByNode = new ObservableGroupMap(this.entries, (e) => `${e.node}`)
    }

    size = 0
    backoff = 0
    lineNumber = 0

    private jobWorkflowProm!: Promise<JobWorkflow>
    private executionStatusProm!: Promise<ExecutionStatusGetResponse>

    /** Get an observable list of entries grouped by node or node and step */
    getEntriesByNodeCtx(node: string, stepCtx?: string) {
        if (stepCtx)
            return this.entriesbyNodeCtx.get(`${node}:${JobWorkflow.cleanContextId(stepCtx)}`)
        else
            return this.entriesByNode.get(node)
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

    @Serial
    @actionAsync
    async getOutput(maxLines: number): Promise<ExecutionOutputEntry[]> {
        const workflow = await task(this.getJobWorkflow())
        await task(this.waitBackOff())

        const res = await task(this.client.executionOutputGet(this.id, {offset: this.offset.toString(), maxlines: maxLines}))

        this.offset = parseInt(res.offset)
        this.size = res.totalSize
        this.completed = res.completed && res.execCompleted
        this.execCompleted = res.execCompleted

        if (res.percentLoaded)
            this.percentLoaded = res.percentLoaded

        if (res.empty)
            this.empty = true

        if (res.error)
            this.error = res.error

        if (!this.completed && res.entries.length == 0) {
            this.increaseBackOff()
        } else {
            this.decreaseBackOff()
        }

        const newEntries: ExecutionOutputEntry[] = []
        for (const entry of res.entries) {
            this.lineNumber++
            const entryObj = ExecutionOutputEntry.FromApiResponse(this, entry, this.lineNumber, workflow)
            newEntries.push(entryObj)
        }
        this.entries.push(...newEntries)
        return newEntries
    }

    private async waitBackOff() {
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

    /**
     * Registers the callback to receive MobX array changes. Use this to ensure same MobX import is used.
     */
    observeEntries(callback: (change: IArrayChange<ExecutionOutputEntry> | IArraySplice<ExecutionOutputEntry>) => void) {
        observe(this.entries, change => {
          callback(change)
        })
    }
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

    static FromApiResponse(executionOutput: ExecutionOutput, resp: ApiExecutionOutputEntry, line: number, workflow: JobWorkflow) {
        const entry = new ExecutionOutputEntry(executionOutput)
        entry.time = resp.time!
        entry.absoluteTime = resp.absoluteTime!
        entry.log = resp.log
        entry.level = resp.level
        // @ts-ignore
        entry.loghtml = resp.loghtml
        entry.stepctx = resp.stepctx
        entry.node = resp.node
        entry.lineNumber = line
        entry.renderedStep = entry.stepctx ? workflow.renderStepsFromContextPath(entry.stepctx) : undefined

        return entry
    }
}