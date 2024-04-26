import { Client } from './rundeck-client/client';
/**
 * Consolidates environment setup and teardown and exposes through
 * two primary methods: up and down.
 */
export declare class EnvBuilder {
    readonly client: Client;
    constructor(client: Client);
    /**
     * Returns after the testing environment is ready.
     */
    up(): Promise<void>;
    /**
     * Returns after the testing environment has been cleaned up.
     */
    down(): Promise<void>;
    /**
     * Continually checks for Rundeck readyness by attempting to login.
     * Throws an error login is not successful within the timeout period.
     */
    waitForRundeckReady(timeout?: number): Promise<void>;
}
