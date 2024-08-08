package org.rundeck.util.common

enum WaitingTime {

    LOW("A Second", 1000),
    MODERATE("Five Seconds", 5000),
    EXCESSIVE("Sixty seconds", 60000)

    public final String label
    public final int milliSeconds

    WaitingTime(String label, int milliseconds) {
        this.label = label
        this.milliSeconds = milliseconds
    }
}