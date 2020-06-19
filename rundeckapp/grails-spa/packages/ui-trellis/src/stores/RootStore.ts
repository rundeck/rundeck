import { ExecutionOutputStore, ExecutionOutput } from "./ExecutionOutput"
import { WorkflowStore } from './Workflow'
import { RundeckClient } from "@rundeck/client"

export class RootStore {
    executionOutputStore: ExecutionOutputStore
    workflowStore: WorkflowStore

    constructor(readonly client: RundeckClient) {
        this.executionOutputStore = new ExecutionOutputStore(this, client)
        this.workflowStore = new WorkflowStore(this, client)
    }
}