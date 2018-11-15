export * from './rundeckService';
export { RundeckBrowser } from 'ts-rundeck';
declare const _default: {
    FilterPrefs: {
        setFilterPref: (key: any, value: any) => Promise<{}>;
        getAvailableFilterPrefs: () => Promise<{}>;
        unsetFilterPref: (key: string) => Promise<{}>;
    };
    Tokens: {
        getUIAjaxTokens: () => Promise<import("./interfaces/rundeckWindow").RundeckToken>;
        setNewUIToken: (responseHeaders: any) => Promise<{}>;
        getToken: (token_name: string) => Promise<{}>;
        setToken: (responseHeaders: any, token_name: string) => Promise<{}>;
    };
};
export default _default;
