package org.rundeck.plugin.example;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;

public class MaxPercentageOrchestator implements Orchestrator {
    public static final Logger logger = Logger.getLogger(MaxPercentageOrchestator.class);

    private final List<INodeEntry> list;
    private int count;
    private final int max;


    public MaxPercentageOrchestator(StepExecutionContext context, Collection<INodeEntry> nodes, int percentage) {
        this.list = new ArrayList<INodeEntry>(nodes);
        percentage = validPercentage(percentage);
        this.max = calculateMax(nodes.size(), percentage);
        if (context != null) {
            context.getExecutionListener().log(3,
                                               "MaxPercentageOrchestator " +
                                               percentage +
                                               "% of " +
                                               nodes.size() +
                                               " Nodes is " +
                                               max
            );
        }

        this.count = 0;
    }

    protected int calculateMax(int nodeCount, int percentage) {
        int valid = validPercentage(percentage);
        int max = Math.round(nodeCount * valid / 100f);
        if (max == 0) {
            max = 1;
        }
        return max;
    }

    private int validPercentage(final int percentage) {
        return Math.min(100, Math.max(0, percentage));
    }


    @Override
    public INodeEntry nextNode() {
        if (count + 1 > max || list.size() < 1) {
            return null;
        }
        count++;
        return list.remove(0);
    }

    @Override
    public void returnNode(final INodeEntry node, boolean success, NodeStepResult result) {
        count--;
    }

    public boolean isComplete() {
        return list.size() == 0;
    }
}
