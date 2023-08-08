package com.dtolabs.rundeck.core.execution.service

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.ExecutionContext

class DummyNodeExecutor implements NodeExecutor{
    @Override
    NodeExecutorResult executeCommand(ExecutionContext context, String[] command, INodeEntry node) {
        return null
    }
}
