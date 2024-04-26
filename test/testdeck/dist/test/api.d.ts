import { RundeckCluster } from '../RundeckCluster';
import { IRequiredResources } from '../TestProject';
export interface ITestContext {
    cluster: RundeckCluster;
}
export declare function CreateRundeckCluster(): Promise<RundeckCluster>;
export declare function CreateTestContext(resources: IRequiredResources): ITestContext;
