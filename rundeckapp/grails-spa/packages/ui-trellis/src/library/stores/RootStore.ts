import { RundeckClient } from "@rundeck/client";
import { reactive, UnwrapNestedRefs } from "vue";
import { api } from "../services/api";
import { ExecutionOutputStore } from "./ExecutionOutput";

import { JobPageStore } from "./JobPageStore";
import { NavBar } from "./NavBar";
import { NewsStore } from "./News";
import { NodeSourceFile } from "./NodeSourceFile";
import { PluginStore } from "./Plugins";
import { ProjectStore } from "./Projects";
import { Releases } from "./Releases";
import { SystemStore } from "./System";
import { ThemeStore } from "./Theme";
import { UIStore } from "./UIStore";
import { UtilityBar } from "./UtilityBar";
import { WebhookStore } from "./Webhooks";
import { WorkflowStore } from "./Workflow";

export class RootStore {
  executionOutputStore: ExecutionOutputStore;
  workflowStore: UnwrapNestedRefs<WorkflowStore>;
  navBar: UnwrapNestedRefs<NavBar>;
  utilityBar: UnwrapNestedRefs<UtilityBar>;
  releases: UnwrapNestedRefs<Releases>;
  system: UnwrapNestedRefs<SystemStore>;
  projects: UnwrapNestedRefs<ProjectStore>;
  news: UnwrapNestedRefs<NewsStore>;
  plugins: UnwrapNestedRefs<PluginStore>;
  webhooks: UnwrapNestedRefs<WebhookStore>;
  theme: UnwrapNestedRefs<ThemeStore>;
  ui: UnwrapNestedRefs<UIStore>;
  nodeSourceFile: UnwrapNestedRefs<NodeSourceFile>;
  jobPageStore: UnwrapNestedRefs<JobPageStore>;
  requestCache: { [key: string]: Promise<any> } = {};

  constructor(
    readonly client: RundeckClient,
    appMeta: any = {},
  ) {
    this.executionOutputStore = new ExecutionOutputStore(this, client);
    this.workflowStore = reactive(new WorkflowStore(this, client));
    this.navBar = reactive(new NavBar(this, client));
    this.utilityBar = reactive(new UtilityBar(this, client));
    this.system = reactive(new SystemStore(this, client));
    this.system.loadMeta(appMeta);
    this.releases = reactive(new Releases(this, client));
    this.projects = reactive(new ProjectStore(this, client));
    this.news = reactive(new NewsStore(this, client));
    this.plugins = reactive(new PluginStore(this, client));
    this.webhooks = reactive(new WebhookStore(this, client));
    this.theme = reactive(new ThemeStore());
    this.ui = reactive(new UIStore());
    this.nodeSourceFile = reactive(new NodeSourceFile(this, client));
    this.jobPageStore = reactive(new JobPageStore());
  }

  /**
   * Get API response for a path, caching the result
   * @param path API url path
   * @returns Promise<any>
   */
  cachedApi(path: string) {
    if (this.requestCache[path] !== undefined) {
      return this.requestCache[path];
    }

    this.requestCache[path] = api.get(path);
    return this.requestCache[path];
  }

  /**
   * Invalidate a cached API response
   * @param path
   */
  invalidatePath(path: string) {
    delete this.requestCache[path];
  }
}
