package com.dtolabs.rundeck.core.rules;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Created by greg on 5/18/16.
 */
public interface WorkflowSystemBuilder {
    WorkflowSystemBuilder ruleEngine(RuleEngine engine);

    WorkflowSystemBuilder state(MutableStateObj state);

    WorkflowSystemBuilder executor(Supplier<ExecutorService> executor);

    WorkflowSystemBuilder listener(WorkflowSystemEventListener listener);

    WorkflowSystem build();
}
