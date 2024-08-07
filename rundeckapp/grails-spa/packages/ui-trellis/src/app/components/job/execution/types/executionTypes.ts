interface PluginInitialData {
  name: string;
  description: string;
  fwkPropertiesMapping?: any;
  highlighted: boolean;
  metadata?: any;
  pluginGroupType?: any;
  properties: any;
  propertiesMapping: any;
}

type Plugin = PluginInitialData & {
  type: string;
  iconUrl?: string;
  providerMetadata?: {
    glyphicon?: string;
    faicon?: string;
    fabicon?: string;
  };
  extra?: {
    config?: any;
    type?: string;
  };
};

interface JobPlugins {
  [key: string]: { [key: string]: any };
}

interface PluginDataFromApi {
  pluginsInitialData: PluginInitialData[];
  ExecutionLifecycle: JobPlugins;
}

export { PluginInitialData, Plugin, JobPlugins, PluginDataFromApi };
