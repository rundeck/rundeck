import { RootStore } from "./RootStore";
import { RundeckClient } from "@rundeck/client";

import { Serial } from "../utilities/Async";

export class PluginStore {
  plugins: Plugin[] = [];

  pluginsByService: { [key: string]: Plugin[] } = {};
  pluginsById: { [key: string]: Plugin[] } = {};

  constructor(
    readonly root: RootStore,
    readonly client: RundeckClient,
  ) {}

  @Serial
  async load(service: string): Promise<void> {
    if (this.pluginsByService[service]) return void 0;
    const plugins = await this.client.apiRequest({
      pathTemplate: "api/40/plugin/list",
      queryParameters: {
        service,
      },
      method: "GET",
    });

    plugins.parsedBody.forEach((p: any) => {
      const pluginKey = this._getPluginByIdKey(p);
      if (this.pluginsById[pluginKey]) return;
      else this.plugins.push(p);
    });
    this._refreshPluginGroups();
    return void 0;
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
      this.pluginsByService[service]?.sort((a, b) =>
        a.title.localeCompare(b.title),
      ) || []
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
}

export enum ServiceType {
  WebhookEvent = "WebhookEvent",
  WorkflowNodeStep = "WorkflowNodeStep",
  WorkflowStep = "WorkflowStep",
  LogFilter = "LogFilter",
}
