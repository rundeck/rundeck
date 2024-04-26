import { Page } from '../page';
import { Context } from '../context';
export declare enum Elems {
    /** @todo This button could use an id */
    submitBtn = "//button[@type=\"submit\"]"
}
export declare const Checkboxes: string[];
export declare const Radios: string[];
export declare class ProjectExportPage extends Page {
    readonly ctx: Context;
    readonly project: string;
    path: string;
    constructor(ctx: Context, project: string);
    getLabel(name: string): Promise<any>;
    getCheckbox(name: string): Promise<any>;
    getRadio(name: string): Promise<any>;
}
