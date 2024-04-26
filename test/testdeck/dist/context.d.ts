import { RundeckCluster } from './RundeckCluster';
import { S3 } from 'aws-sdk';
import { WebDriver } from 'selenium-webdriver';
export declare class Context {
    readonly driverProvider: () => Promise<WebDriver>;
    readonly baseUrl: string;
    readonly s3Upload: boolean;
    readonly s3Base: string;
    Rundeck: RundeckCluster;
    currentTestName: string;
    s3: S3;
    uploadPromises: Promise<{}>[];
    snapCounter: number;
    contextId: string;
    driver: WebDriver;
    constructor(driverProvider: () => Promise<WebDriver>, baseUrl: string, s3Upload: boolean, s3Base: string);
    init(): Promise<void>;
    urlFor(path: string): string;
    friendlyTestName(): string;
    screenshot(): Promise<any>;
    dispose(): Promise<void>;
    screenSnap(name: string): Promise<any>;
    screenCapToS3(screen: string, name: string): Promise<void>;
}
