import { ExecutionOutputStore, ExecutionOutput } from "./ExecutionOutput"
import { WorkflowStore } from './Workflow'
import { NavBar } from './NavBar'
import { UtilityBar } from './UtilityBar'
import { RundeckClient } from "@rundeck/client"

export class RootStore {
    executionOutputStore: ExecutionOutputStore
    workflowStore: WorkflowStore
    navBar: NavBar
    utilityBar: UtilityBar

    constructor(readonly client: RundeckClient) {
        this.executionOutputStore = new ExecutionOutputStore(this, client)
        this.workflowStore = new WorkflowStore(this, client)
        this.navBar = new NavBar(this, client)
        this.utilityBar = new UtilityBar(this, client)
    }
}