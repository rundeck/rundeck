package com.dtolabs.rundeck.core.jobs;

public class JobLifeCycleStatus {

    private boolean successFul;
    private String description;

    public boolean isSuccessFul() {
        return successFul;
    }

    public void setSuccessFul(boolean successFul) {
        this.successFul = successFul;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public JobLifeCycleStatus(boolean successFul, String description) {
        this.successFul = successFul;
        this.description = description;
    }

    public JobLifeCycleStatus() {}
}
