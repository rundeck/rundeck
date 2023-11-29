package org.rundeck.plugin.util;

import com.dtolabs.rundeck.core.common.IExecutionProviders;
import com.dtolabs.rundeck.core.execution.ExecutionServiceImpl;

public class ScriptFileExecutionServiceImpl extends ExecutionServiceImpl {
    private final IExecutionProviders executionProviders;

    public ScriptFileExecutionServiceImpl(IExecutionProviders executionProviders) {
        this.executionProviders = executionProviders;
    }

    @Override
    public IExecutionProviders getExecutionProviders() {
        return this.executionProviders;
    }
}
