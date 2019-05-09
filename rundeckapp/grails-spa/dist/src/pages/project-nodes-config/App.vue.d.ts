import Vue from 'vue';
import { RundeckContext } from "@rundeck/ui-trellis";
import PluginValidation from '@rundeck/ui-trellis/src/interfaces/PluginValidation';
interface PluginConf {
    readonly type: string;
    config: any;
}
interface ProjectPluginConfigEntry {
    entry: PluginConf;
    extra: PluginConf;
    validation: PluginValidation;
    create?: boolean;
}
declare const _default: import("vue").VueConstructor<{
    title: string;
    mode: string;
    project: string;
    rdBase: string;
    configPrefix: string;
    serviceName: string;
    cancelUrl: string;
    pluginConfigs: ProjectPluginConfigEntry[];
    configOrig: any[];
    additionalProps: any;
    rundeckContext: RundeckContext;
    modalAddOpen: boolean;
    pluginProviders: never[];
    pluginLabels: {};
    editFocus: number;
    errors: string[];
} & {
    notifyError(msg: string, args: any[]): void;
    notifySuccess(title: string, msg: string): void;
    createConfigEntry(entry: any): ProjectPluginConfigEntry;
    serializeConfigEntry(entry: ProjectPluginConfigEntry): any;
    addPlugin(provider: string): void;
    setFocus(focus: number): void;
    savePlugin(plugin: ProjectPluginConfigEntry, index: number): Promise<void>;
    removePlugin(plugin: ProjectPluginConfigEntry, index: string): void;
    movePlugin(index: number, plugin: ProjectPluginConfigEntry, shift: number): void;
    savePlugins(): Promise<void>;
    loadProjectPluginConfig(project: string, configPrefix: string, serviceName: string): Promise<any>;
    saveProjectPluginConfig(project: string, configPrefix: string, serviceName: string, data: ProjectPluginConfigEntry[]): Promise<{
        success: boolean;
        data: any;
    } | {
        success: boolean;
        data?: undefined;
    }>;
    cancelAction(): void;
} & Record<never, any> & Vue>;
export default _default;
