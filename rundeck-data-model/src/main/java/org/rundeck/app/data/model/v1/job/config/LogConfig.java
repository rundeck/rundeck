package org.rundeck.app.data.model.v1.job.config;

public interface LogConfig {
    String getLoglevel();
    String getLogOutputThreshold();
    String getLogOutputThresholdAction();
    String getLogOutputThresholdStatus();
}
