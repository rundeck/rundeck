package com.dtolabs.rundeck.core.jobs;

public class JobLifeCycleStatus {

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

    public JobLifeCycleStatus(boolean successful, String description) {
        this.successful = successful;
        this.description = description;
    }

    public JobLifeCycleStatus() {}
}
