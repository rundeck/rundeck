package org.rundeck.util.api

enum WaitingTime {

    LOW("A Second", 1000)

    public final String label
    public final int milliSeconds

    WaitingTime(String label, int seconds) {
        this.label = label
        this.milliSeconds = seconds
    }
}