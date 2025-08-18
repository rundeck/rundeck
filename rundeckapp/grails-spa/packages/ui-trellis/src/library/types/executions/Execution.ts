/**
 * Note: migrated from @rundeck/client
 */
/**
 * An interface representing ExecutionDateStarted.
 */
export interface ExecutionDateStarted {
  unixtime?: number;
  date?: string;
}

export declare enum Status {
  Running = "running",
  Succeeded = "succeeded",
  Failed = "failed",
  Aborted = "aborted",
  Timedout = "timedout",
  FailedWithRetry = "failed-with-retry",
  Scheduled = "scheduled",
  Other = "other",
}

export interface JobMetadata {
  id?: string;
  name?: string;
  group?: string;
  project?: string;
  description?: string;
  href?: string;
  permalink?: string;
  scheduled?: boolean;
  scheduleEnabled?: boolean;
  averageDuration?: number;
  options?: any;
}

export interface Execution {
  id?: number;
  href?: string;
  permalink?: string;
  /**
   * Possible values include: 'running', 'succeeded', 'failed', 'aborted', 'timedout',
   * 'failed-with-retry', 'scheduled', 'other'
   */
  status?: Status;
  customStatus?: string;
  project?: string;
  user?: string;
  serverUUID?: string;
  dateStarted?: ExecutionDateStarted;
  job?: JobMetadata;
  description?: string;
  argstring?: string;
  successfulNodes?: string[];
}
