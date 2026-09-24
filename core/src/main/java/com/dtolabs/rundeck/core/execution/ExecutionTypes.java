package com.dtolabs.rundeck.core.execution;

/**
 * The values written to an Execution's {@code executionType}. Nothing outside this set is
 * produced by Rundeck, though rows predating the column may hold null.
 */
public class ExecutionTypes {
    /** Quartz schedule trigger, and every adhoc command run. */
    public static final String SCHEDULED = "scheduled";
    /** A user running a job directly. */
    public static final String USER = "user";
    /** A user scheduling a job for later ("Run Later"). */
    public static final String USER_SCHEDULED = "user-scheduled";
    /**
     * A single arbitrary plugin step run from the Ad Hoc Step page, without a job. Distinct
     * from {@link #SCHEDULED} so these can be kept out of the pages that list job and adhoc
     * command activity; identifying them by the absence of a job would also catch adhoc
     * commands and executions orphaned by a deleted job.
     */
    public static final String ADHOC_STEP = "adhoc-step";

    private ExecutionTypes() {
    }
}
