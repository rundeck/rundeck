package org.rundeck.plugin.scriptnodestep

import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepException
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult

class Util {
    static void handleFailureResult(NodeStepResult nodeExecutorResult, INodeEntry entry) throws NodeStepException {
        if (!nodeExecutorResult.isSuccess()) {
            throw new NodeStepException(
                nodeExecutorResult.failureMessage ?: ('Step failed: ' + nodeExecutorResult.failureReason),
                nodeExecutorResult.failureReason,
                entry.getNodename()
            )
        }
    }
}
