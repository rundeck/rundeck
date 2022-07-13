import { ExecutionOutputStore, ExecutionOutput } from "./ExecutionOutput"
import { WorkflowStore } from './Workflow'
import { NavBar } from './NavBar'
import { UtilityBar } from './UtilityBar'
import { SystemStore } from './System'
import { RundeckClient } from "@rundeck/client"
import { Releases } from "./Releases"
import { ProjectStore } from "./Projects"
import { NewsStore } from './News'
import { PluginStore } from './Plugins'
import { WebhookStore } from './Webhooks'
import { ThemeStore } from "./Theme"

export class RootStore {
    executionOutputStore: ExecutionOutputStore
    workflowStore: WorkflowStore
    navBar: NavBar
    utilityBar: UtilityBar
    releases: Releases
    system: SystemStore
    projects: ProjectStore
    news: NewsStore
    plugins: PluginStore
    webhooks: WebhookStore
    theme: ThemeStore

    constructor(readonly client: RundeckClient, appMeta: any = {}) {
        this.executionOutputStore = new ExecutionOutputStore(this, client)
        this.workflowStore = new WorkflowStore(this, client)
        this.navBar = new NavBar(this, client)
        this.utilityBar = new UtilityBar(this, client)
        this.system = new SystemStore(this, client)
        this.system.loadMeta(appMeta)
        this.releases = new Releases(this, client)
        this.projects = new ProjectStore(this, client)
        this.news = new NewsStore(this, client)
        this.plugins = new PluginStore(this, client)
        this.webhooks = new WebhookStore(this, client)
        this.theme = new ThemeStore()
    }
}