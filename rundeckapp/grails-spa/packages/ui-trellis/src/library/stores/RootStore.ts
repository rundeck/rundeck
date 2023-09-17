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
import { ThemeStore } from './Theme'
import { NodeSourceFile } from './NodeSourceFile'
import { UIStore } from './UIStore'
import {reactive, UnwrapNestedRefs} from "vue";

export class RootStore {
    executionOutputStore: ExecutionOutputStore
    workflowStore: UnwrapNestedRefs<WorkflowStore>
    navBar: UnwrapNestedRefs<NavBar>
    utilityBar: UnwrapNestedRefs<UtilityBar>
    releases: UnwrapNestedRefs<Releases>
    system: UnwrapNestedRefs<SystemStore>
    projects: UnwrapNestedRefs<ProjectStore>
    news: UnwrapNestedRefs<NewsStore>
    plugins: UnwrapNestedRefs<PluginStore>
    webhooks: UnwrapNestedRefs<WebhookStore>
    theme: UnwrapNestedRefs<ThemeStore>
    ui: UnwrapNestedRefs<UIStore>
    nodeSourceFile: UnwrapNestedRefs<NodeSourceFile>

    constructor(readonly client: RundeckClient, appMeta: any = {}) {
        this.executionOutputStore = new ExecutionOutputStore(this, client)
        this.workflowStore = reactive(new WorkflowStore(this, client))
        this.navBar = reactive(new NavBar(this, client))
        this.utilityBar = reactive(new UtilityBar(this, client))
        this.system = reactive(new SystemStore(this, client))
        this.system.loadMeta(appMeta)
        this.releases = reactive(new Releases(this, client))
        this.projects = reactive(new ProjectStore(this, client))
        this.news = reactive(new NewsStore(this, client))
        this.plugins = reactive(new PluginStore(this, client))
        this.webhooks = reactive(new WebhookStore(this, client))
        this.theme = reactive(new ThemeStore())
        this.ui = reactive(new UIStore())
        this.nodeSourceFile = reactive(new NodeSourceFile(this, client))
    }
}