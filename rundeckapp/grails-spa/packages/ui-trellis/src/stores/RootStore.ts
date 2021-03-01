import { ExecutionOutputStore, ExecutionOutput } from "./ExecutionOutput"
import { WorkflowStore } from './Workflow'
import { NavBar } from './NavBar'
import { RundeckClient } from "@rundeck/client"

export class RootStore {
    executionOutputStore: ExecutionOutputStore
    workflowStore: WorkflowStore
    navBar: NavBar

    constructor(readonly client: RundeckClient) {
        this.executionOutputStore = new ExecutionOutputStore(this, client)
        this.workflowStore = new WorkflowStore(this, client)
        this.navBar = new NavBar(this, client)
    }
}