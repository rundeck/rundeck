import { Page } from '../page';
import { Context } from '../context';
export declare const Elems: {
    jobTitleLink: any;
    jobUuidText: any;
    jobDescription: any;
    optionInput: any;
};
export declare class JobShowPage extends Page {
    readonly ctx: Context;
    readonly project: string;
    readonly jobid: string;
    path: string;
    constructor(ctx: Context, project: string, jobid: string);
    jobTitleLink(): Promise<any>;
    jobDescription(): Promise<any>;
    jobDescriptionText(): Promise<any>;
    jobTitleText(): Promise<any>;
    jobUuidText(): Promise<any>;
    optionInputText(name: string): Promise<any>;
}
