import { RundeckClient } from "@rundeck/client";
import { getPluginDetail } from "../services/plugins";

import { Serial } from "../utilities/Async";
import { RootStore } from "./RootStore";
import { apiClient } from "../services/api";

export class PluginStore {
  plugins: Plugin[] = [];

  pluginsByService: { [key: string]: Plugin[] } = {};
  pluginsById: { [key: string]: Plugin[] } = {};
  pluginDetailLoader: { [key: string]: Promise<any> } = {};

  constructor(
    readonly root: RootStore,
    readonly client: RundeckClient,
  ) {}

  @Serial
  async load(service: string): Promise<void> {
    if (this.pluginsByService[service]) return void 0;
    if (
      service === ServiceType.WorkflowNodeStep ||
      service === ServiceType.WorkflowStep
    ) {
        this._injectStaticPlugins(service);
    }
    const plugins = await apiClient(51).get("plugin/list", {
      params: { service },
    });

    plugins.data.forEach((p: any) => {
      const pluginKey = this._getPluginByIdKey(p);
      if (this.pluginsById[pluginKey]) return;
      else this.plugins.push(p);
    });
    this._refreshPluginGroups();
    return void 0;
  }

    private _injectStaticPlugins(service: string) {
        const jobRefPlugin = {
            artifactName: "Job reference",
            author: "",
            builtin: true,
            id: "",
            name: "job.reference",
            pluginVersion: "",
            service: service,
            description: service === ServiceType.WorkflowNodeStep
                ? "Run a job on the remote node"
                : "Execute another job",
            title: "Job reference",
            providerMetadata: {
                glyphicon: "book",
            },
            isHighlighted: true,
            highlightedOrder: 5,
        };
        const conditionalLogicPlugin = {
            artifactName: "Conditional logic",
            author: "",
            builtin: true,
            id: "",
            name: "conditional.logic",
            pluginVersion: "0.1",
            service: service,
            description: `Define steps that will be executed ${service === ServiceType.WorkflowNodeStep? 'per-node ': ''}under specific conditions`,
            title: `Conditional Logic ${service === ServiceType.WorkflowNodeStep? 'Node': 'Workflow'} Step`,
            providerMetadata: {
                glyphicon: "book",
            },
            isHighlighted: true,
            highlightedOrder: 6,
        };

        const staticPlugins = [jobRefPlugin, conditionalLogicPlugin];

        for(const staticPlugin of staticPlugins) {
            const pluginKey = this._getPluginByIdKey(staticPlugin);
            if (!this.pluginsById[pluginKey]) {
                this.plugins.push(staticPlugin);
            }
        }
    }

    /**
   * Get the plugin detail for a service provider, caching the result
   * @param serviceName
   * @param provider
   */
  getPluginDetail(serviceName: string, provider: string) {
    if (this.pluginDetailLoader[`${serviceName}/${provider}`] !== undefined) {
      return this.pluginDetailLoader[`${serviceName}/${provider}`];
    }

    this.pluginDetailLoader[`${serviceName}/${provider}`] = getPluginDetail(
      serviceName,
      provider,
    );
    return this.pluginDetailLoader[`${serviceName}/${provider}`];
  }

  _refreshPluginGroups() {
    this.pluginsByService = this.plugins.reduce((r, p) => {
      const svcKey = `${p.service}`;
      r[svcKey] = r[svcKey] || [];
      r[svcKey].push(p);
      return r;
    }, Object.create(null));
    this.pluginsById = this.plugins.reduce((r, p) => {
      const svcKey = this._getPluginByIdKey(p);
      r[svcKey] = r[svcKey] || [];
      r[svcKey].push(p);
      return r;
    }, Object.create(null));
  }

  _getPluginByIdKey(plugin: Plugin) {
    if (plugin.name && plugin.service) {
      return `${plugin.name}-${plugin.service}`;
    }
    return plugin.name;
  }

  getServicePlugins(service: string): Plugin[] {
    return (
      this.pluginsByService[service]?.sort((a, b) => {
        if (a.isHighlighted !== undefined && b.isHighlighted !== undefined) {
          if (a.isHighlighted !== b.isHighlighted) {
            return a.isHighlighted ? -1 : 1;
          }

          if (a.isHighlighted && b.isHighlighted) {
            return a.highlightedOrder! - b.highlightedOrder!;
          }
        }
        return a.title.localeCompare(b.title);
      }) || []
    );
  }
}

export interface Plugin {
  id: string;
  name: string;
  artifactName: string;
  title: string;
  description: string;
  author: string;
  builtin: boolean;
  pluginVersion: string;
  service: string;
  iconUrl?: string;
  providerMetadata?: {
    glyphicon?: string;
    faicon?: string;
    fabicon?: string;
  };
  isHighlighted?: boolean;
  highlightedOrder?: number;
}

export enum ServiceType {
  WebhookEvent = "WebhookEvent",
  WorkflowNodeStep = "WorkflowNodeStep",
  WorkflowStep = "WorkflowStep",
  LogFilter = "LogFilter",
}
