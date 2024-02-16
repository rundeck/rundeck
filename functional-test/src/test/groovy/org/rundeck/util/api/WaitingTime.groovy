package org.rundeck.util.api

enum WaitingTime {

    LOW("A Second", 1000),
    MODERATE("Five Seconds", 5000),
    EXCESSIVE("Sixty seconds", 60000)

    public final String label
    public final int milliSeconds

    WaitingTime(String label, int seconds) {
        this.label = label
        this.milliSeconds = seconds
    }
}