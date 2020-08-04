import {observable, action, runInAction} from 'mobx'

import {RundeckClient} from '@rundeck/client'
import {
    ExecutionState as ApiExecState,
    ExecutionStateWorkflow,
    ExecutionStateStep} from '@rundeck/client/dist/lib/models'

import { RootStore } from './RootStore'

enum ExecutionStepState {
    PENDING = 'PENDING',
    WAITING = 'WAITING',
    RUNNING = 'RUNNING',
    SUCCEEDED = 'SUCCEEDED',
    FAILED = 'FAILED'
}

export class ExecutionStateStore {
    @observable.shallow
    executionStates: Map<string, ExecutionState> = new Map()

    constructor(readonly root: RootStore, readonly client: RundeckClient) {}

    @action
    async fetch(id: string) {
        const state = this.getOrCreate(id)

        const data = await this.client.executionStateGet(id)
        state.updateFromApiReponse(data)

        return state
    }

    getOrCreate(id: string) {
        let state = this.executionStates.get(id)
        if (!state) {
            state = new ExecutionState(this)
            this.executionStates.set(id, state)
        }
        return state
    }
}

export class ExecutionState {
    executionId!: number
    serverNode!: ExecutionNode

    @observable
    state!: ExecutionStepState

    @observable
    completed: boolean = false

    @observable.shallow
    nodes: Map<string, ExecutionNode> = new Map()
    
    @observable.shallow
    workflow!: ExecutionWorkflow

    @observable.shallow
    steps: Map<string, ExecutionStep> = new Map()

    constructor(readonly store: ExecutionStateStore) {}

    @action
    updateFromApiReponse(data: ApiExecState) {
        this.executionId = data.executionId
        this.serverNode = this.getOrCreateNode(data.serverNode!)

        if (! data.executionState) {
            this.state = ExecutionStepState.PENDING
            return
        }

        this.state = ExecutionStepState[data.executionState]

        if ( ! this.workflow)
            this.workflow = new ExecutionWorkflow(this)

        const {workflow} = this

        /* Execution state API resp is a workflow with extra properties */
        workflow.updateFromApiCall(data)

        if (data.nodes) {
            for (const nodeName in data.nodes) {
                const node = this.nodes.get(nodeName)

                if (!node)
                    continue

                const stepStates = data.nodes[nodeName]
                for (const stepState of stepStates) {
                    const step = this.steps.get(stepState.stepctx)
                    if (!step)
                        continue

                    node.addStep(step)
                }
            }
        }
    }

    getOrCreateNode(name: string) {
        let node = this.nodes.get(name)
        if (!node) {
            node = new ExecutionNode(name)
            this.nodes.set(name, node)
        }
        return node
    }

    getOrCreateStep(stepctx: string, workflow: ExecutionWorkflow) {
        let step = this.steps.get(stepctx)
        if (!step) {
            step = new ExecutionStep(workflow)
            this.steps.set(stepctx, step)
        }
        return step
    }
}

export class ExecutionWorkflow {
    steps: Array<ExecutionStep> = []

    stepCount!: number

    @observable
    completed: boolean = false

    @observable
    startTime?: Date

    @observable
    endTime?: Date

    @observable.shallow
    targetNodes: Array<ExecutionNode> = []

    @observable.shallow
    nodes: Map<string, ExecutionNode> = new Map()

    constructor(readonly executionState: ExecutionState) {}

    @action
    updateFromApiCall(data: ExecutionStateWorkflow) {
 
        this.completed = data.completed

        this.startTime = parseMaybeDate(data.startTime)
        this.endTime = parseMaybeDate(data.endTime)
        
        this.stepCount = data.stepCount!

        for (const node of data.allNodes!) {
            this.nodes.set(node, this.executionState.getOrCreateNode(node))
        }

        for (const stepData of data.steps!) {
            const step = this.getOrCreateStep(stepData.stepctx)
            step.updateFromApiResponse(stepData)
        }
    }

    getOrCreateStep(stepctx: string) {
        let step = this.steps.find(s => s.stepctx == stepctx)
        if (!step) {
            step = this.executionState.getOrCreateStep(stepctx, this)
            this.steps.push(step)
        }
        return step
    }
}

export class ExecutionStep {
    id!: string
    stepctx!: string
    nodeStep!: boolean

    @observable
    state!: ExecutionStepState

    duration!: number
    startTime?: Date
    updateTime?: Date
    endTime?: Date

    parameters: Map<string, string> = new Map()

    @observable.shallow
    parameterStates: Map<string, ExecutionStep> = new Map()

    hasSubWorkflow: boolean = false
    subWorkflow?: ExecutionWorkflow

    constructor(readonly workflow: ExecutionWorkflow) {}

    updateFromApiResponse(data: ExecutionStateStep) {
        this.id = data.id
        this.stepctx = data.stepctx

        this.state = ExecutionStepState[data.executionState]

        this.duration = data.duration
        this.startTime = parseMaybeDate(data.startTime)
        this.endTime = parseMaybeDate(data.endTime)
        this.updateTime = parseMaybeDate(data.updateTime)

        this.hasSubWorkflow = new Boolean(data.hasSubworkflow).valueOf()
        this.nodeStep = data.nodeStep

        if (this.hasSubWorkflow && this.nodeStep) {
            for (const key in data.parameterStates) {
                const state = data.parameterStates[key]
                const parameterStep = new ExecutionStep(this.workflow)
                parameterStep.updateFromApiResponse(state)
                this.parameterStates.set(key, parameterStep)
            }
        }

        if (this.hasSubWorkflow && data.workflow) {
            const workflow = new ExecutionWorkflow(this.workflow.executionState)
            this.subWorkflow = workflow
            workflow.updateFromApiCall(data.workflow)
        }
    }
}

export class ExecutionNode {
    /** List of workflow steps in the order they were executed on this node */
    @observable.shallow
    steps: Array<ExecutionStep> = []

    @observable.shallow
    stepsByCtx: Map<string, ExecutionStep> = new Map()

    /** Unique name of this node */
    name: string

    constructor(name: string) {
        this.name = name
    }

    /** Add associated step if it has not already been added */
    addStep(step: ExecutionStep) {
        let storedStep = this.stepsByCtx.get(step.stepctx)
        if (!storedStep) {
            console.log(step.stepctx)
            this.steps.push(step)
            this.stepsByCtx.set(step.stepctx, step)
        }
    }
}

function parseMaybeDate(date: string | null | undefined) {
    return date ? new Date(date) : undefined
}