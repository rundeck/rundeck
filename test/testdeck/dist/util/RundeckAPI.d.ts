import { WebResource, RequestPolicy, RequestPolicyFactory, RequestPolicyOptions, BaseRequestPolicy, HttpOperationResponse } from '@azure/ms-rest-js';
import { Rundeck } from '@rundeck/client';
import { RundeckJobExecutionRunOptionalParams } from '@rundeck/client/dist/lib/models';
export declare function waitForRundeckReady(client: Rundeck, timeout?: number): Promise<void>;
export declare function createWaitForRundeckReady(factory: () => Rundeck, timeout?: number): Promise<void>;
export declare function waitForExecutionComplete(client: Rundeck, id: number): Promise<ExecutionStatusGetResponse>;
export declare function runJobAndWait(client: Rundeck, id: string, options?: RundeckJobExecutionRunOptionalParams): Promise<ExecutionStatusGetResponse>;
export declare function cookieEnrichPolicy(cookies: string[]): RequestPolicyFactory;
/** Enriches each request with a set of cookies */
export declare class CookieEnrichPolicy extends BaseRequestPolicy {
    readonly cookies: string[];
    constructor(nextPolicy: RequestPolicy, options: RequestPolicyOptions, cookies: string[]);
    sendRequest(webResource: WebResource): Promise<HttpOperationResponse>;
}
