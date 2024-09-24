package org.rundeck.util.common

import java.time.Duration

/**
 * Collection of common time durations for waiting.
 */
final class WaitingTime {

    /** Waiting time of 1 second */
    public static final Duration LOW = Duration.ofSeconds(1)

    /** Waiting time of 5 seconds */
    public static final Duration MODERATE = Duration.ofSeconds(5)

    /** Waiting time of 1 minute */
    public static final Duration EXCESSIVE = Duration.ofSeconds(60)

    /** Waiting time of 2 minutes */
    public static final Duration XTRA_EXCESSIVE = Duration.ofSeconds(120)

    /** Waiting time of 2 minutes */
    public static final Duration XTRA_EXCESSIVE_LONG_JOBS = Duration.ofSeconds(1200)

    // prevent instantiation.
    private WaitingTime() {}
}