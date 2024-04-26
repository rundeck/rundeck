import { Rundeck } from "@rundeck/client";
export interface IRequiredResources {
    projects?: string[];
}
export declare class TestProject {
    static LoadResources(client: Rundeck, resources: IRequiredResources): Promise<void>;
    static ImportProject(client: Rundeck, project: string): Promise<void>;
    static ExportProject(client: Rundeck, project: string): Promise<void>;
}
