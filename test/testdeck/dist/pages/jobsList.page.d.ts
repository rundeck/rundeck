import { Page } from '../page';
import { Context } from '../context';
export declare const Elems: {
    modalOptions: any;
    searchModalFields: any;
    runFormButton: any;
    jobSearchButton: any;
    jobSearchNameField: any;
    jobSearchGroupField: any;
    jobSearchSubmitButton: any;
    jobRowLinks: any;
    optionValidationWarningText: any;
};
export declare class JobsListPage extends Page {
    readonly ctx: Context;
    readonly project: string;
    path: string;
    constructor(ctx: Context, project: string);
    getRunJobLink(uuid: string): Promise<any>;
    searchJobName(name: string, group: string): Promise<any>;
    getJobSearchButton(): Promise<any>;
    getJobSearchSubmitButton(): Promise<any>;
    getJobSearchNameField(): Promise<any>;
    getJobSearchGroupField(): Promise<any>;
    getJobsRowLinkElements(): Promise<any>;
    getRunJobNowLink(): Promise<any>;
    getOptionWarningText(): Promise<any>;
}
