/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.dtolabs.rundeck.core.execution.dispatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeStepResult;
import com.dtolabs.rundeck.plugins.orchestrator.Orchestrator;

/**
 * OrchestratorNodeProcessor is the class that deals with the concurrent processing of the jobs
 *
 * @author Ashley Taylor
 */
public class OrchestratorNodeProcessor {
    private volatile boolean stop;
    private final int threadCount;
    private final boolean keepgoing;
    private final Orchestrator orchestrator;
    private final Map<INodeEntry, Callable<NodeStepResult>> executions;
    private final ExecutorService threadPool;

    private Set<INodeEntry> processedNodes;
    
    public OrchestratorNodeProcessor(int threadCount, boolean keepgoing,
            Orchestrator orchestrator,
            Map<INodeEntry, Callable<NodeStepResult>> executions) {
        stop = false;
        this.threadCount = threadCount;
        this.keepgoing = keepgoing;
        this.orchestrator = orchestrator;
        this.executions = executions;

        this.threadPool = Executors.newFixedThreadPool(this.threadCount);
        
        this.processedNodes = Collections.newSetFromMap(new ConcurrentHashMap<INodeEntry, Boolean>());

    }

    public void execute() throws ExecutionException{
        List<OrchestratorRunnable> tasks = new ArrayList<OrchestratorRunnable>();
        for (int i = 0; i < this.threadCount; i++) {
            tasks.add(new OrchestratorRunnable());
        }
        try {
            for(Future<Boolean> future: threadPool.invokeAll(tasks)){
                future.get();
            }
        } catch (InterruptedException e) {
            
        }finally{
            threadPool.shutdown();
        }
    }

    public class OrchestratorRunnable implements Callable<Boolean> {

        @Override
        public Boolean call() throws Exception {
            
            while (!stop) {
                Entry task = null;
                try {
                    //make sure the get callable happens within the try catch so the finally will always be called
                    task = getCallable();
                    if(task == null) {
                        break;
                    }
                    NodeStepResult result = task.callable.call();
                    if (!result.isSuccess() && !keepgoing) {
                        stop = true;
                        break;
                    }
                } catch (Exception e) {
                    if (!keepgoing) {
                        stop = true;
                        throw e;
                    }
                } finally {
                    if(task != null) {
                        returnNode(task.node);
                    }
                }
            }
            return true;
        }
    }

    public Entry getCallable() throws DispatcherException {
        INodeEntry node = this.orchestrator.getNode();
        if(node == null){
            return null;
        }
        
        if(!this.processedNodes.add(node)){
            throw new DispatcherException("Can not process the same node twice " + node);
        }
        
        Callable<NodeStepResult> callable = this.executions.get(node);
        return new Entry(node, callable);
    }

    public void returnNode(INodeEntry node) {
        this.orchestrator.returnNode(node);
    }

    public static class Entry {
        private final INodeEntry node;
        private final Callable<NodeStepResult> callable;

        public Entry(INodeEntry node, Callable<NodeStepResult> callable) {
            super();
            this.node = node;
            this.callable = callable;
        }

    }

}
