package com.dtolabs.rundeck.core.execution.workflow;

public interface WorkflowMetricsWriter {
    void markMeterStepMetric(String classname, String metricName);
}
