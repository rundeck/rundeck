/*
 * Copyright 2026 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * Computes, for a single top-level execution, the elapsed time and dispatch count of every
 * step-node combination that ran, broken down per step and keyed by each step's hierarchical
 * path (the same scheme {@code state.json} uses for its own step identifiers). A step that
 * dispatches to one or more nodes contributes at the node level only, once per node; a step
 * that never dispatches to any node contributes its own duration once.
 * <p>
 * Instances are constructed per top-level execution, with that execution's id
 * closure-captured. A nested job-reference execution shares the same listener instance as its
 * parent, so its step/node durations accumulate into the same running breakdown; the parent's
 * {@link #finishWorkflowExecution} always fires after any nested one, so its write to
 * {@link StepNodeUsageStore} is always the one that persists.
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
     * One step's contribution: its elapsed time in nanoseconds, the plugin/provider type that
     * ran, whether it is a node-dispatching step, and how many (step, node) dispatches
     * contributed to the duration.
     */
    public static final class StepNodeUsageEntry {
        private final long nanos;
        private final String pluginType;
        private final Boolean isNodeStep;
        private final long nodeCount;

        public StepNodeUsageEntry(final long nanos, final String pluginType, final Boolean isNodeStep, final long nodeCount) {
            this.nanos = nanos;
            this.pluginType = pluginType;
            this.isNodeStep = isNodeStep;
            this.nodeCount = nodeCount;
        }

        /**
         * This step's total elapsed time, in nanoseconds. Kept at full precision here
         * deliberately -- a caller that needs a whole-seconds total across multiple steps
         * should sum this field first and round once, rather than rounding each step
         * individually (which undercounts many small steps summing to a real total).
         */
        public long getNanos() {
            return nanos;
        }

        /**
         * The plugin/provider type that ran for this step, or null if it couldn't be
         * determined.
         */
        public String getPluginType() {
            return pluginType;
        }

        /**
         * Whether this step is a node-dispatching step. Null under the same
         * "couldn't be determined" condition as {@link #getPluginType()}.
         */
        public Boolean getIsNodeStep() {
            return isNodeStep;
        }

        /**
         * How many (step, node) dispatches contributed to {@link #getNanos()}.
         */
        public long getNodeCount() {
            return nodeCount;
        }

        @Override
        public String toString() {
            return "StepNodeUsageEntry{nanos=" + nanos + ", pluginType='" + pluginType + "', isNodeStep=" + isNodeStep + ", nodeCount=" + nodeCount + "}";
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
     * nested job-reference's own workflow invoked from step 3. Tolerant of a null
     * {@code getStepContext()} (treated as no parent path).
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
     * the item is a node step, otherwise {@link StepExecutionItem#getType()}.
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
            final String path = entry.getKey();
            final LongAdder nodeCountAdder = perStepNodeCount.get(path);
            final long nodeCount = nodeCountAdder != null ? nodeCountAdder.sum() : 0L;
            breakdown.put(path, new StepNodeUsageEntry(entry.getValue().sum(), perStepPluginType.get(path), perStepIsNodeStep.get(path), nodeCount));
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
        final long now = System.nanoTime();
        final StepFrame previous = openStepFrames.put(new StepKey(item), new StepFrame(now, path, type, nodeStep));
        if (previous != null && !previous.sawNodeDispatch.get()) {
            // A second begin for the same item instance, with no finish in between, would
            // otherwise silently discard the still-open frame and lose its elapsed time --
            // finalize it instead.
            addElapsed(previous.path, now - previous.startNanos);
            addNodeCount(previous.path);
        }
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
