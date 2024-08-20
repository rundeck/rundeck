package org.rundeck.util.common

import java.time.Duration

enum WaitingTime {

    LOW("A Second", Duration.ofSeconds(1)),
    MODERATE("Five Seconds", Duration.ofSeconds(5)),
    EXCESSIVE("Sixty seconds", Duration.ofSeconds(60)),
    XTRA_EXCESSIVE("One-hundred twenty seconds", Duration.ofSeconds(120))


    public final String label
    public final Duration duration

    WaitingTime(String label, Duration duration) {
        this.label = label
        this.duration = duration
    }
}