package com.dtolabs.rundeck.core.execution.workflow.steps.node;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.HasSharedContext;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason;

import java.util.Map;

/**
 * Created by greg on 6/3/16.
 */
public class NodeStepDataResultImpl extends NodeStepResultImpl implements HasSharedContext, ChainedNodeStepResult {
    WFSharedContext sharedContext;
    private final NodeStepResult original;

    public NodeStepDataResultImpl(
            final NodeStepResult original,
            final Throwable exception,
            final FailureReason failureReason,
            final String failureMessage,
            final Map<String, Object> failureData,
            final INodeEntry node,
            final WFSharedContext sharedContext
    )
    {
        super(exception, failureReason, failureMessage, failureData, node);
        this.sharedContext = sharedContext;
        this.original = original;
        setSuccess(original.isSuccess());
        setSourceResult(original);

    }

    /**
     * Add a data context to a source result
     *
     * @param result
     * @param dataContext
     *
     * @return
     */
    public static NodeStepResult with(final NodeStepResult result, final WFSharedContext dataContext) {
        return new NodeStepDataResultImpl(
                result,
                result.getException(),
                result.getFailureReason(),
                result.getFailureMessage(),
                result.getFailureData(),
                result.getNode(),
                dataContext
        );
    }

    @Override
    public WFSharedContext getSharedContext() {
        return sharedContext;
    }

    public NodeStepResult getOriginal() {
        return original;
    }

    @Override
    public String toString() {
        return super.toString() + " + {dataContext=" + sharedContext + "} ";
    }
}
