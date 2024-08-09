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
  validation?: {
    valid?: boolean;
    errors?: {
      [key: string]: any;
    };
  };
};

interface JobPlugins {
  [key: string]: { [key: string]: any };
}

interface PluginDataFromApi {
  pluginsInitialData: PluginInitialData[];
  ExecutionLifecycle: JobPlugins;
  validationErrors?: any;
}

export { PluginInitialData, Plugin, JobPlugins, PluginDataFromApi };
