import {observable} from 'mobx'

export class ExecutionOutputStore {
    @observable executionOutputsById: Map<string, number> = new Map()
}

export class ExecutionOutput {
    id!: string
    offset!: string
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

    @observable.shallow entries: ExecutionOutputEntry[] = []

    @observable.shallow entriesByNode: Map<string, ExecutionOutputEntry[]> = new Map()

    constructor() {}
}

class ExecutionOutputEntry {
    time!: string
    absoluteTime!: string
    log!: string
    logHtml?: string
    level?: string
    stepctx?: string
    node?: string
    lineNumber!: number

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