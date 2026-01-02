/**
 * AdhocCommandStore - State management for adhoc command execution
 * 
 * Replaces Knockout.js AdhocCommand binding with Vue 3 reactive state
 */

export interface AdhocLink {
  href: string | null;
  title: string | null;
  execid: string | null;
  filter: string | null;
  extraMetadata: any | null;
  status: string | null;
  succeeded: boolean | null;
  statusClass: string;
}

export interface RecentCommand extends AdhocLink {
  fillCommand: () => void;
}

const statusMap: Record<string, string> = {
  scheduled: "running",
  running: "running",
  succeed: "succeed",
  succeeded: "succeed",
  fail: "fail",
  failed: "fail",
  cancel: "aborted",
  aborted: "aborted",
  retry: "failedretry",
  timedout: "timedout",
  timeout: "timedout",
};

function getStatusClass(status: string | null): string {
  if (!status) return "other";
  return statusMap[status] || "other";
}

export class AdhocCommandStore {
  private _commandString: string = "";
  private _recentCommands: RecentCommand[] = [];
  private _recentCommandsLoaded: boolean = false;
  private _running: boolean = false;
  private _canRun: boolean = false;
  private _error: string | null = null;
  private _followControl: any = null;
  private _loadMax: number = 20;

  // Callback for when node filter total changes
  private _onNodeFilterTotalChange?: (total: number) => void;

  constructor(initialData?: { commandString?: string }) {
    if (initialData?.commandString) {
      this._commandString = initialData.commandString;
    }
  }

  get commandString(): string {
    return this._commandString;
  }

  set commandString(value: string) {
    this._commandString = value;
  }

  get recentCommands(): RecentCommand[] {
    return this._recentCommands;
  }

  get recentCommandsLoaded(): boolean {
    return this._recentCommandsLoaded;
  }

  get recentCommandsNoneFound(): boolean {
    return this._recentCommands.length < 1 && this._recentCommandsLoaded;
  }

  get running(): boolean {
    return this._running;
  }

  set running(value: boolean) {
    this._running = value;
  }

  get canRun(): boolean {
    return this._canRun;
  }

  set canRun(value: boolean) {
    this._canRun = value;
  }

  get allowInput(): boolean {
    return !this._running && this._canRun;
  }

  get error(): string | null {
    return this._error;
  }

  set error(value: string | null) {
    this._error = value;
  }

  get followControl(): any {
    return this._followControl;
  }

  set followControl(value: any) {
    this._followControl = value;
  }

  get loadMax(): number {
    return this._loadMax;
  }

  /**
   * Set callback for node filter total changes
   */
  setNodeFilterTotalCallback(callback: (total: number) => void) {
    this._onNodeFilterTotalChange = callback;
  }

  /**
   * Update canRun state based on node filter total
   */
  updateCanRunFromNodeTotal(total: number | string, running: boolean = false): void {
    if (running) {
      this._canRun = false;
      return;
    }
    
    // Handle both number and string cases (for backwards compatibility)
    const totalNum = typeof total === "string" ? parseInt(total, 10) : total;
    if (totalNum && totalNum !== 0 && total !== "0") {
      this._canRun = true;
    } else {
      this._canRun = false;
    }

    if (this._onNodeFilterTotalChange) {
      this._onNodeFilterTotalChange(totalNum);
    }
  }

  /**
   * Load recent commands from API
   */
  async loadRecentCommands(
    apiUrl: string,
    nodeFilterStore?: { setSelectedFilter: (filter: string) => void }
  ): Promise<void> {
    try {
      this._recentCommandsLoaded = false;
      this._error = null;

      const response = await fetch(`${apiUrl}?max=${this._loadMax}`);
      if (!response.ok) {
        throw new Error(`Request failed: ${response.statusText}`);
      }

      const data = await response.json();
      this._recentCommandsLoaded = true;

      if (data.executions && Array.isArray(data.executions)) {
        this._recentCommands = data.executions.map((exec: any) => {
          const link: RecentCommand = {
            href: exec.href || null,
            title: exec.title || null,
            execid: exec.execid || null,
            filter: exec.filter || null,
            extraMetadata: exec.extraMetadata || null,
            status: exec.status || null,
            succeeded: exec.succeeded || null,
            statusClass: getStatusClass(exec.status),
            fillCommand: () => {
              this._commandString = exec.title || "";
              if (nodeFilterStore && exec.filter) {
                nodeFilterStore.setSelectedFilter(exec.filter);
              }
              // Emit runner-filter-changed event
              if (exec.extraMetadata) {
                const runnerEvent = new CustomEvent("runner-filter-changed", {
                  detail: exec.extraMetadata,
                });
                window.dispatchEvent(runnerEvent);
              }
            },
          };
          return link;
        });
      } else {
        this._recentCommands = [];
      }
    } catch (err: any) {
      this._recentCommandsLoaded = true;
      this._error = `Recent commands list: request failed: ${err.message}`;
      console.error("Recent commands list: error receiving data", err);
      this._recentCommands = [];
    }
  }

  /**
   * Reset store to initial state
   */
  reset(): void {
    this._commandString = "";
    this._recentCommands = [];
    this._recentCommandsLoaded = false;
    this._running = false;
    this._canRun = false;
    this._error = null;
    this._followControl = null;
  }

  /**
   * Stop following execution output
   */
  stopFollowing(): void {
    if (this._followControl && typeof this._followControl.stopFollowingOutput === "function") {
      this._followControl.stopFollowingOutput();
    }
  }
}

