export interface PluginConfig<
  TConfig extends object = Record<string, unknown>,
> {
  type: string;
  config: TConfig;
}
