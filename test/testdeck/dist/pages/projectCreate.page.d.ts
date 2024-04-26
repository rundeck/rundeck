import { Page } from '../page';
export declare const Elems: {
    projectNameInput: any;
    labelInput: any;
    descriptionInput: any;
};
export declare class ProjectCreatePage extends Page {
    path: string;
    projectNameInput(): Promise<any>;
    labelInput(): Promise<any>;
    descriptionInput(): Promise<any>;
}
