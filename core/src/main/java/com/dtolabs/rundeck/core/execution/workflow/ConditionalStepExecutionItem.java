package com.dtolabs.rundeck.core.execution.workflow;

import java.util.Map;

/**
 * Created by greg on 4/28/16.
 */
public interface ConditionalStepExecutionItem {
    Map<String, Object> getConditionsMap();
}
