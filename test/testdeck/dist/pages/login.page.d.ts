import { Context } from '../context';
import { Page } from '../page';
export declare enum Elems {
    username = "//*[@id=\"login\"]",
    password = "//*[@id=\"password\"]",
    /** @todo This button could use an id */
    loginBtn = "//*[@id=\"btn-login\"]"
}
export declare class LoginPage extends Page {
    readonly ctx: Context;
    path: string;
    constructor(ctx: Context);
    get(): Promise<void>;
    sendLogin(username: string, password: string): Promise<void>;
    login(username: string, password: string): Promise<void>;
    badLogin(username: string, password: string): Promise<void>;
}
