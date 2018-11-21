import { RundeckBrowser } from 'ts-rundeck';
export interface RundeckContext {
    rdBase: string;
    apiVersion: string;
    projectName: string;
    activeTour: string;
    activeTourStep: string;
    token: RundeckToken;
    tokens: {
        [key: string]: RundeckToken;
    };
    rundeckClient: RundeckBrowser;
}
export interface RundeckToken {
    TOKEN: string;
    URI: string;
}
