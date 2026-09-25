package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import com.dtolabs.rundeck.core.execution.StatusResult;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

/**
 * Computes "step_node_seconds" for a single top-level execution: the sum, across every
 * step and every node it dispatched to, of the elapsed duration of each step-node
 * combination that ran -- broken down per step, not collapsed into one grand total, so a
 * caller (e.g. a future billing subscriber excluding "non-billable" steps) can tell which
 * step contributed how much, and which plugin it ran (so exclusion can be driven by a
 * plugin-type deny-list rather than needing a per-step flag in every job definition). Also
 * tracks a discrete step-node dispatch count alongside the duration, for a duration-independent
 * metric candidate (RBA_BILLING proposal Section 6.5) -- see {@link StepNodeUsageEntry#getNodeCount()}.
 * <p>
 * A step that dispatches to one or more nodes contributes at the node level only (one
 * contribution per node it ran on, summed into that step's own entry); a step that never
 * dispatches to any node (a plain control-flow/notification step, or a node-dispatching step
 * whose filter matched zero nodes) contributes its own duration once, as a single
 * step-times-one-node unit.
 * <p>
 * Instances are constructed per top-level execution with that execution's id
 * closure-captured (mirroring how other per-execution listeners are built in
 * {@code ExecutionService.executeAsyncBegin}). A nested job-reference execution shares the
 * exact same listener instance as its parent, so its step/node durations accumulate into
 * this same running breakdown automatically. {@link #finishWorkflowExecution} fires once for
 * such a nested execution's own completion, and again for the parent's -- both write the
 * breakdown (whatever it is at that moment) into {@link StepNodeUsageStore} under the one
 * closure-captured top-level execution id; since the nested execution's finish always
 * happens strictly before the parent's own, the parent's later write is always the one that
 * persists, with no nesting-depth tracking required.
 * <p>
 * Each step's contribution is keyed by its hierarchical step path -- the same mechanism the
 * execution engine already uses internally to build {@code state.json}'s own step
 * identifiers ({@link StepExecutionContext#getStepContext()} plus
 * {@link StepExecutionContext#getStepNumber()}), not a new one invented for this class. This
 * correctly disambiguates a nested job-reference's own step numbering (which restarts at 1)
 * from an outer step of the same number, unlike a flat {@code stepNumber} alone would.
 * (step, node) *open-frame* bookkeeping (matching a begin to its finish while a dispatch is
 * in flight) still uses Java reference identity of the {@link StepExecutionItem} instance,
 * exactly as before -- only the key under which a *finished* duration is stored changed.
 */
public class StepNodeUsageWorkflowListener implements WorkflowExecutionListener {

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
        final String path;
        final String pluginType;
        final boolean isNodeStep;
        final AtomicBoolean sawNodeDispatch = new AtomicBoolean(false);

        StepFrame(final long startNanos, final String path, final String pluginType, final boolean isNodeStep) {
            this.startNanos = startNanos;
            this.path = path;
            this.pluginType = pluginType;
            this.isNodeStep = isNodeStep;
        }
    }

    private static final class NodeFrame {
        final long startNanos;
        final String path;
        final String pluginType;
        final boolean isNodeStep;

        NodeFrame(final long startNanos, final String path, final String pluginType, final boolean isNodeStep) {
            this.startNanos = startNanos;
            this.path = path;
            this.pluginType = pluginType;
            this.isNodeStep = isNodeStep;
        }
    }

    /**
     * One step's contribution to step_node_seconds: its duration, the plugin/provider type
     * that ran (e.g. what {@link StepExecutionItem#getType()} -- or
     * {@link NodeStepExecutionItem#getNodeStepType()} for a node step -- returns) so a
     * consumer can decide billability by plugin type without needing to cross-reference the
     * job definition at all, and how many step-node units contributed to {@link #seconds}
     * (RBA_BILLING proposal Section 6.5 -- raised by Greg Schueler, 2026-09-25): a discrete
     * count of (step, node) dispatches, the same breadth×depth shape as step_node_seconds but
     * without the time dimension, since on-prem billing likely shouldn't charge for the
     * customer's own hardware speed. One node-dispatching step run against 5 nodes
     * contributes 5 here, not 1 -- the same "1 step x 1 node" convention as
     * {@link #seconds} applies to a step that never dispatches to any node.
     */
    public static final class StepNodeUsageEntry {
        private final long seconds;
        private final String pluginType;
        private final Boolean isNodeStep;
        private final long nodeCount;

        /**
         * @deprecated use {@link #StepNodeUsageEntry(long, String, Boolean, long)}. This
         * overload defaults {@code nodeCount} to 1, for callers (mostly tests) that don't
         * care about the node-count breakdown.
         */
        @Deprecated
        public StepNodeUsageEntry(final long seconds, final String pluginType, final Boolean isNodeStep) {
            this(seconds, pluginType, isNodeStep, 1L);
        }

        public StepNodeUsageEntry(final long seconds, final String pluginType, final Boolean isNodeStep, final long nodeCount) {
            this.seconds = seconds;
            this.pluginType = pluginType;
            this.isNodeStep = isNodeStep;
            this.nodeCount = nodeCount;
        }

        public long getSeconds() {
            return seconds;
        }

        /**
         * The plugin/provider type that ran for this step, or null if it couldn't be
         * determined (e.g. an unmatched finish call in a test, with no corresponding begin).
         */
        public String getPluginType() {
            return pluginType;
        }

        /**
         * Whether this step is a node-dispatching step ({@link NodeStepExecutionItem}) --
         * i.e. whether {@link #getPluginType()} came from {@code getNodeStepType()} (true) or
         * {@code getType()} (false) -- so a consumer knows which of
         * {@code NonBillableStepTypesProvider}'s two categories to check {@link #pluginType}
         * against. Null under the same "couldn't be determined" condition as
         * {@link #getPluginType()}.
         */
        public Boolean getIsNodeStep() {
            return isNodeStep;
        }

        /**
         * How many (step, node) dispatches contributed to {@link #seconds} -- see the class
         * javadoc. Not currently read by any billing consumer; captured for a future
         * duration-independent metric candidate (e.g. on-prem consumption billing).
         */
        public long getNodeCount() {
            return nodeCount;
        }

        @Override
        public String toString() {
            return "StepNodeUsageEntry{seconds=" + seconds + ", pluginType='" + pluginType + "', isNodeStep=" + isNodeStep + ", nodeCount=" + nodeCount + "}";
        }
    }

    private final Long executionId;
    private final ConcurrentHashMap<String, LongAdder> perStepElapsedNanos = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LongAdder> perStepNodeCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> perStepPluginType = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> perStepIsNodeStep = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<StepKey, StepFrame> openStepFrames = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<NodeKey, NodeFrame> openNodeFrames = new ConcurrentHashMap<>();

    public StepNodeUsageWorkflowListener(final Long executionId) {
        this.executionId = executionId;
    }

    /**
     * The step's hierarchical path, e.g. "3" for a top-level step 3, or "3/1" for step 1 of a
     * nested job-reference's own workflow invoked from step 3 -- the same identifier scheme
     * {@code state.json} already uses for its own step entries. Tolerant of a null
     * {@code getStepContext()} (treated as no parent path) so a partially-stubbed test
     * context still produces a stable, if less specific, key rather than throwing.
     */
    private static String stepPath(final StepExecutionContext context) {
        final StringBuilder sb = new StringBuilder();
        final List<Integer> parents = context.getStepContext();
        if (parents != null) {
            for (final Integer parent : parents) {
                sb.append(parent).append('/');
            }
        }
        sb.append(context.getStepNumber());
        return sb.toString();
    }

    /**
     * The step's plugin/provider type: {@link NodeStepExecutionItem#getNodeStepType()} when
     * the item is a node step (the more specific of the two for that case), otherwise
     * {@link StepExecutionItem#getType()}.
     */
    private static String pluginType(final StepExecutionItem item) {
        if (item instanceof NodeStepExecutionItem) {
            return ((NodeStepExecutionItem) item).getNodeStepType();
        }
        return item.getType();
    }

    /**
     * Whether the item is a node-dispatching step -- the same {@code instanceof} check
     * {@link #pluginType(StepExecutionItem)} branches on, kept in sync with it deliberately.
     */
    private static boolean isNodeStep(final StepExecutionItem item) {
        return item instanceof NodeStepExecutionItem;
    }

    private void addElapsed(final String path, final long nanos) {
        perStepElapsedNanos.computeIfAbsent(path, k -> new LongAdder()).add(nanos);
    }

    private void addNodeCount(final String path) {
        perStepNodeCount.computeIfAbsent(path, k -> new LongAdder()).increment();
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
        final Map<String, StepNodeUsageEntry> breakdown = new LinkedHashMap<>();
        for (final Map.Entry<String, LongAdder> entry : perStepElapsedNanos.entrySet()) {
            final long seconds = Math.round(entry.getValue().sum() / 1_000_000_000.0);
            final String path = entry.getKey();
            final LongAdder nodeCountAdder = perStepNodeCount.get(path);
            final long nodeCount = nodeCountAdder != null ? nodeCountAdder.sum() : 0L;
            breakdown.put(path, new StepNodeUsageEntry(seconds, perStepPluginType.get(path), perStepIsNodeStep.get(path), nodeCount));
        }
        StepNodeUsageStore.getInstance().recordFinishedBreakdown(executionId, breakdown);
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
        final String path = stepPath(context);
        final String type = pluginType(item);
        final boolean nodeStep = isNodeStep(item);
        // ConcurrentHashMap disallows null values -- a step type that can't be determined
        // (e.g. an unstubbed test double) just leaves no entry, rather than recording "null".
        if (type != null) {
            perStepPluginType.putIfAbsent(path, type);
            perStepIsNodeStep.putIfAbsent(path, nodeStep);
        }
        openStepFrames.put(new StepKey(item), new StepFrame(System.nanoTime(), path, type, nodeStep));
    }

    @Override
    public void finishStepExecution(final StepExecutor executor, final StatusResult result, final StepExecutionContext context, final StepExecutionItem item) {
        final StepFrame frame = openStepFrames.remove(new StepKey(item));
        if (frame != null && !frame.sawNodeDispatch.get()) {
            addElapsed(frame.path, System.nanoTime() - frame.startNanos);
            addNodeCount(frame.path);
        }
    }

    @Override
    public void beginExecuteNodeStep(final ExecutionContext context, final NodeStepExecutionItem item, final INodeEntry node) {
        final StepFrame frame = openStepFrames.get(new StepKey(item));
        String path = null;
        String type = null;
        boolean nodeStep = true; // this callback only ever fires for a NodeStepExecutionItem
        if (frame != null) {
            frame.sawNodeDispatch.set(true);
            path = frame.path;
            type = frame.pluginType;
            nodeStep = frame.isNodeStep;
        }
        openNodeFrames.put(new NodeKey(item, node.getNodename()), new NodeFrame(System.nanoTime(), path, type, nodeStep));
    }

    @Override
    public void finishExecuteNodeStep(final NodeStepResult result, final ExecutionContext context, final StepExecutionItem item, final INodeEntry node) {
        final NodeFrame frame = openNodeFrames.remove(new NodeKey(item, node.getNodename()));
        if (frame != null) {
            final String path = frame.path != null ? frame.path : ("unknown:" + System.identityHashCode(item));
            addElapsed(path, System.nanoTime() - frame.startNanos);
            addNodeCount(path);
            if (frame.pluginType != null) {
                perStepPluginType.putIfAbsent(path, frame.pluginType);
                perStepIsNodeStep.putIfAbsent(path, frame.isNodeStep);
            }
        }
    }
}
