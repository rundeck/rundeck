package org.rundeck.plugin.example;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;

public class MaxPercentageOrchestator implements Orchestrator {
    public static final Logger logger = Logger.getLogger(MaxPercentageOrchestator.class);

	private final ConcurrentLinkedQueue<INodeEntry> queue;
	private final AtomicInteger count;
	private final int max;
	
	
	public MaxPercentageOrchestator(StepExecutionContext context, Collection<INodeEntry> nodes, int percentage) {
		this.queue = new ConcurrentLinkedQueue<INodeEntry>(nodes);
		this.max = calculateMax(nodes.size(), percentage);
		if(context != null){
		    context.getExecutionListener().log(3, "MaxPercentageOrchestator " + percentage + "% of " + nodes.size() + " Nodes is " + max);
		}
		
		this.count = new AtomicInteger(0);
	}
	
	protected int calculateMax(int nodeCount, int percentage){
	    int max = Math.round(nodeCount * percentage/100f);
	    if(max == 0){
	        max = 1;
	    }
	    return max;
	}
	

	@Override
	public INodeEntry getNode() {
        if(count.incrementAndGet() > max){
            count.decrementAndGet();
            return null;
        }
	    return queue.poll();
	}

	@Override
	public void returnNode(INodeEntry node) {
	    count.decrementAndGet();
	}

}
