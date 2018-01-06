package com.dtolabs.rundeck.core.execution.workflow.steps;

import java.util.Map;

public interface DynamicPropertiesStepPlugin {
    Map<String, Object> getDynamicProperties(Map<String, Object> projectAndFrameworkValues);
}
