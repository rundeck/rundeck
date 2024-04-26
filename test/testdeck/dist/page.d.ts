import { Context } from './context';
import { By } from 'selenium-webdriver';
export declare abstract class Page {
    readonly ctx: Context;
    abstract path: string;
    constructor(ctx: Context);
    get(): Promise<void>;
    clickBy(by: By): Promise<void>;
    screenshot(freeze?: boolean): Promise<any>;
    /** Attempt to freeze the page for stable screenshots */
    freeze(): Promise<void>;
    /** Hides version box for screenshots */
    hideSidebarBottom(): Promise<void>;
    hideTests(): Promise<void>;
    hideServerUuid(): Promise<void>;
    /** Hide spinners */
    hideSpinners(): Promise<void>;
    /** Attempts to clear transitions, animations, and animated gifs from the page */
    disableTransitions(): Promise<void>;
    /** Blur the active element. Useful for hiding blinking cursor before screen cap. */
    blur(): Promise<void>;
}
