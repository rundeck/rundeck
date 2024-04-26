export interface ITest {
    name: string;
    file: string;
}
export interface ITestGroup {
    name: string;
    tests: ITest[];
}
/**
 * Represents the discovered test folder.
 */
export declare class TestRepo {
    readonly groups: ITestGroup[];
    /** Returns a TestRepo constructed from the supplied path. */
    static CreateTestRepo(path: string, filter: RegExp): Promise<TestRepo>;
    private static _loadRepo;
    private static _loadRepoFolder;
    private static _dirContents;
    private static _statFiles;
    constructor(groups: ITestGroup[]);
}
