package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Computes "step_node_seconds" for a single top-level execution: the sum, across every
 * step and every node it dispatched to, of the elapsed duration of each step-node
 * combination that ran.
 * <p>
 * A step that dispatches to one or more nodes contributes at the node level only (one
 * contribution per node it ran on); a step that never dispatches to any node (a plain
 * control-flow/notification step, or a node-dispatching step whose filter matched zero
 * nodes) contributes its own duration once, as a single step-times-one-node unit.
 * <p>
 * Instances are constructed per top-level execution with that execution's id
 * closure-captured (mirroring how other per-execution listeners are built in
 * {@code ExecutionService.executeAsyncBegin}). A nested job-reference execution shares the
 * exact same listener instance as its parent, so its step/node durations accumulate into
 * this same running total automatically. {@link #finishWorkflowExecution} fires once for
 * such a nested execution's own completion, and again for the parent's -- both write the
 * running total (whatever it is at that moment) into {@link StepNodeSecondsStore} under the
 * one closure-captured top-level execution id; since the nested execution's finish always
 * happens strictly before the parent's own, the parent's later write is always the one that
 * persists, with no nesting-depth tracking required.
 * <p>
 * (step, node) identity is keyed by Java reference identity of the {@link StepExecutionItem}
 * instance (plus node name for node-level keys), not by flat step number: a nested
 * job-reference's own workflow restarts step numbering at 1, so a (stepNumber, nodeName)
 * key could otherwise collide with an outer step of the same number targeting the same
 * node. Distinct step/node-step instances in the workflow's command list -- including a
 * nested job's own, entirely separate object graph -- can never collide under reference
 * identity.
 */
public class StepNodeSecondsWorkflowListener implements WorkflowExecutionListener {

    private static final class StepKey {
        private final Object item;

        StepKey(final Object item) {
            this.item = item;
        }

        @Override
        public boolean equals(final Object o) {
            return o instanceof StepKey && this.item == ((StepKey) o).item;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(item);
        }
    }

    private static final class NodeKey {
        private final Object item;
        private final String nodeName;

        NodeKey(final Object item, final String nodeName) {
            this.item = item;
            this.nodeName = nodeName;
        }

        @Override
        public boolean equals(final Object o) {
            if (!(o instanceof NodeKey)) {
                return false;
            }
            final NodeKey k = (NodeKey) o;
            return this.item == k.item && Objects.equals(this.nodeName, k.nodeName);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(item) * 31 + Objects.hashCode(nodeName);
        }
    }

    private static final class StepFrame {
        final long startNanos;
        final AtomicBoolean sawNodeDispatch = new AtomicBoolean(false);

        StepFrame(final long startNanos) {
            this.startNanos = startNanos;
        }
    }

    private final Long executionId;
    private final AtomicLong totalElapsedNanos = new AtomicLong();
    private final ConcurrentHashMap<StepKey, StepFrame> openStepFrames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeKey, Long> openNodeStartNanos = new ConcurrentHashMap<>();

    public StepNodeSecondsWorkflowListener(final Long executionId) {
        this.executionId = executionId;
    }

    @Override
    public void beginWorkflowExecution(final StepExecutionContext executionContext, final WorkflowExecutionItem item) {
    }

    @Override
    public void finishWorkflowExecution(
            final WorkflowExecutionResult result,
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
    ) {
        final long seconds = Math.round(totalElapsedNanos.get() / 1_000_000_000.0);
        StepNodeSecondsStore.getInstance().recordFinishedTotal(executionId, seconds);
    }

    @Override
    public void beginWorkflowItem(final int step, final StepExecutionItem item) {
    }

    @Override
    public void beginWorkflowItemErrorHandler(final int step, final StepExecutionItem item) {
    }

    @Override
    public void finishWorkflowItem(final int step, final StepExecutionItem item, final StepExecutionResult result) {
    }

    @Override
    public void finishWorkflowItemErrorHandler(final int step, final StepExecutionItem item, final StepExecutionResult success) {
    }

    @Override
    public void beginStepExecution(final StepExecutor executor, final StepExecutionContext context, final StepExecutionItem item) {
        openStepFrames.put(new StepKey(item), new StepFrame(System.nanoTime()));
    }

    @Override
    public void finishStepExecution(final StepExecutor executor, final StatusResult result, final StepExecutionContext context, final StepExecutionItem item) {
        final StepFrame frame = openStepFrames.remove(new StepKey(item));
        if (frame != null && !frame.sawNodeDispatch.get()) {
            totalElapsedNanos.addAndGet(System.nanoTime() - frame.startNanos);
        }
    }

    @Override
    public void beginExecuteNodeStep(final ExecutionContext context, final NodeStepExecutionItem item, final INodeEntry node) {
        final StepFrame frame = openStepFrames.get(new StepKey(item));
        if (frame != null) {
            frame.sawNodeDispatch.set(true);
        }
        openNodeStartNanos.put(new NodeKey(item, node.getNodename()), System.nanoTime());
    }

    @Override
    public void finishExecuteNodeStep(final NodeStepResult result, final ExecutionContext context, final StepExecutionItem item, final INodeEntry node) {
        final Long start = openNodeStartNanos.remove(new NodeKey(item, node.getNodename()));
        if (start != null) {
            totalElapsedNanos.addAndGet(System.nanoTime() - start);
        }
    }
}
