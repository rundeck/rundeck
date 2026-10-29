import type PluginValidation from "../../../../../library/interfaces/PluginValidation";

interface PluginMetadata {
  glyphicon?: string;
  faicon?: string;
  fabicon?: string;
  [key: string]: unknown;
}

interface PluginInitialData {
  name: string;
  description: string;
  fwkPropertiesMapping?: Record<string, unknown>;
  highlighted: boolean;
  metadata?: PluginMetadata;
  pluginGroupType?: string | null;
  properties: Array<Record<string, unknown>>;
  propertiesMapping: Record<string, unknown>;
}

type Plugin = PluginInitialData & {
  type: string;
  iconUrl?: string;
  providerMetadata?: PluginMetadata;
  extra?: {
    config?: Record<string, unknown>;
    type?: string;
  };
  validation?: PluginValidation;
};

interface JobPlugins {
  [key: string]: Record<string, Record<string, unknown>>;
}

interface PluginDataFromApi {
  pluginsInitialData: PluginInitialData[];
  ExecutionLifecycle: Record<string, Record<string, unknown>>;
  validationErrors?: Record<string, PluginValidation>;
}

export { PluginInitialData, Plugin, JobPlugins, PluginDataFromApi };
