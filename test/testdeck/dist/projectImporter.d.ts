import { Rundeck } from '@rundeck/client';
export declare enum KeyType {
    Public = "publicKey",
    Private = "privateKey",
    Password = "password"
}
export declare class ProjectImporter {
    readonly repoPath: string;
    readonly projectName: string;
    readonly client: Rundeck;
    constructor(repoPath: string, projectName: string, client: Rundeck);
    importProject(): Promise<void>;
}
