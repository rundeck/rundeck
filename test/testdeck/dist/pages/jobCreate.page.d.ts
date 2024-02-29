import { WebElementPromise } from 'selenium-webdriver';
import { Page } from '../page';
import { Context } from '../context';
export declare const Elems: {
    jobNameInput: any;
    groupPathInput: any;
    descriptionTextarea: any;
    saveButton: any;
    editSaveButton: any;
    errorAlert: any;
    formValidationAlert: any;
    tabWorkflow: any;
    addNewWfStepCommand: any;
    wfStepCommandRemoteText: any;
    wfStep0SaveButton: any;
    wfstep0vis: any;
    optionNewButton: any;
    option0EditForm: any;
    option0NameInput: any;
    optionFormSaveButton: any;
    option0li: any;
};
export declare class JobCreatePage extends Page {
    readonly ctx: Context;
    readonly project: string;
    path: string;
    projectName: string;
    constructor(ctx: Context, project: string);
    editPagePath(jobId: string): string;
    getEditPage(jobId: string): Promise<void>;
    jobNameInput(): Promise<any>;
    groupPathInput(): Promise<any>;
    descriptionTextarea(): Promise<any>;
    saveButton(): WebElementPromise;
    editSaveButton(): Promise<any>;
    errorAlert(): WebElementPromise;
    tabWorkflow(): Promise<any>;
    addNewWfStepCommand(): Promise<any>;
    waitWfStepCommandRemoteText(): Promise<void>;
    wfStepCommandRemoteText(): Promise<any>;
    wfStep0SaveButton(): Promise<any>;
    wfstep0vis(): Promise<any>;
    waitWfstep0vis(): Promise<void>;
    optionNewButton(): Promise<any>;
    waitoption0EditForm(): Promise<any>;
    option0NameInput(): Promise<any>;
    optionFormSaveButton(): Promise<any>;
    waitOption0li(): Promise<any>;
    formValidationAlert(): WebElementPromise;
}