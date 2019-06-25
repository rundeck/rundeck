package com.dtolabs.rundeck.core.jobs;

public class JobEventStatus {

    private boolean successful;
    private String description;

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobEventStatus(boolean successful, String description) {
        this.successful = successful;
        this.description = description;
    }

    public JobEventStatus() {}
}
