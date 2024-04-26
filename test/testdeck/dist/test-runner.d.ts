import { ITest, ITestGroup, TestRepo } from './test-repo';
interface ITestResult {
    test: ITest;
    stdout: string;
    stderr: string;
    success: boolean;
}
interface ITestGroupResult {
    testGroup: ITestGroup;
    testResults: ITestResult[];
}
export declare class BitScriptRunner {
    readonly testRepo: TestRepo;
    constructor(testRepo: TestRepo);
    run(): Promise<void>;
    testGroups(): Promise<ITestGroupResult[]>;
    summary(resultGroups: ITestGroupResult[]): void;
}
export {};
