import { RundeckToken } from '../interfaces/rundeckWindow';
export declare const getUIAjaxTokens: () => Promise<RundeckToken>;
export declare const setNewUIToken: (responseHeaders: any) => Promise<{}>;
export declare const getToken: (token_name: string) => Promise<{}>;
export declare const setToken: (responseHeaders: any, token_name: string) => Promise<{}>;
declare const _default: {
    getUIAjaxTokens: () => Promise<RundeckToken>;
    setNewUIToken: (responseHeaders: any) => Promise<{}>;
    getToken: (token_name: string) => Promise<{}>;
    setToken: (responseHeaders: any, token_name: string) => Promise<{}>;
};
export default _default;
