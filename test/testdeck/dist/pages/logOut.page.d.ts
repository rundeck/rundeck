import { Context } from '../context';
import { Page } from '../page';
export declare enum Elems {
    logoutMessageCss = "#loginpage p.text-center.h4"
}
export declare class LogoutPage extends Page {
    readonly ctx: Context;
    path: string;
    constructor(ctx: Context);
    logout(): Promise<void>;
    getLogoutMessage(): Promise<void>;
}
