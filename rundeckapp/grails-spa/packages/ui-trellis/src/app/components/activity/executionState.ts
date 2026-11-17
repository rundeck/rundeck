/**
 * Execution status values the Activity views render with a built-in icon.
 */
export const KNOWN_EXECUTION_STATUSES = [
  "scheduled",
  "running",
  "queued",
  "succeed",
  "succeeded",
  "failed",
  "missed",
  "cancel",
  "aborted",
  "retry",
  "timedout",
  "timeout",
  "fail",
];

const EXECUTION_STATES: Record<string, string> = {
  scheduled: "scheduled",
  succeed: "succeeded",
  succeeded: "succeeded",
  fail: "failed",
  failed: "failed",
  cancel: "aborted",
  aborted: "aborted",
  running: "running",
  queued: "queued",
  missed: "missed",
  timedout: "timedout",
  retry: "failed-with-retry",
};

/**
 * Normalizes an execution or report status to the state name used by the
 * Activity status icons; unknown (custom) statuses map to "other".
 * @param status raw execution status
 * @returns normalized execution state
 */
export function executionState(status?: string): string {
  return (status && EXECUTION_STATES[status]) || "other";
}

/**
 * Whether a status is a user-defined custom status rather than a built-in one.
 * @param status raw execution status
 * @returns true for custom statuses
 */
export function isCustomStatus(status?: string): boolean {
  return KNOWN_EXECUTION_STATUSES.indexOf(executionState(status)) < 0;
}
