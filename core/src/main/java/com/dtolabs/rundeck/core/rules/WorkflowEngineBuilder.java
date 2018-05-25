package com.dtolabs.rundeck.core.rules;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * Created by greg on 5/18/16.
 */
public class WorkflowEngineBuilder implements WorkflowSystemBuilder {
    RuleEngine engine;
    MutableStateObj state;
    Supplier<ExecutorService> executor;
    WorkflowSystemEventListener listener;

    public static WorkflowEngineBuilder builder(WorkflowEngineBuilder source) {
        WorkflowEngineBuilder workflowSystemBuilder = new WorkflowEngineBuilder();
        workflowSystemBuilder.engine = source.engine;
        workflowSystemBuilder.state = source.state;
        workflowSystemBuilder.executor = source.executor;
        workflowSystemBuilder.listener = source.listener;
        return workflowSystemBuilder;
    }

    public static WorkflowEngineBuilder builder() {
        return new WorkflowEngineBuilder();
    }


    @Override
    public WorkflowEngineBuilder ruleEngine(RuleEngine engine) {
        this.engine = engine;
        return this;
    }

    @Override
    public WorkflowEngineBuilder state(MutableStateObj state) {
        this.state = state;
        return this;
    }

    @Override
    public WorkflowEngineBuilder executor(Supplier<ExecutorService> executor) {
        this.executor = executor;
        return this;
    }

    @Override
    public WorkflowEngineBuilder listener(WorkflowSystemEventListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public WorkflowSystem build() {
        if (null == engine || null == state || null == executor) {
            throw new IllegalArgumentException();
        }
        WorkflowEngine workflowEngine = new WorkflowEngine(engine, state, executor.get());
        workflowEngine.setListener(listener);
        return workflowEngine;
    }

}
