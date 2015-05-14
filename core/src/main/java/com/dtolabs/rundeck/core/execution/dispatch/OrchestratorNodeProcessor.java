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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

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
    private BlockingQueue<Result> resultqueue;
    private BlockingQueue<Entry> taskqueue ;

    public OrchestratorNodeProcessor(int threadCount, boolean keepgoing,
            Orchestrator orchestrator,
            Map<INodeEntry, Callable<NodeStepResult>> executions) {
        stop = false;
        if(threadCount<1) {
            throw new IllegalArgumentException("threadCount must be greater than 0: " + threadCount);
        }
        this.threadCount = threadCount;
        this.resultqueue = new LinkedBlockingQueue<>();
        this.taskqueue = new LinkedBlockingQueue<>(threadCount);
        this.keepgoing = keepgoing;
        this.orchestrator = orchestrator;
        this.executions = executions;

        this.threadPool = Executors.newFixedThreadPool(this.threadCount);

        this.processedNodes = Collections.newSetFromMap(new ConcurrentHashMap<INodeEntry, Boolean>());

    }

    public boolean execute() throws ExecutionException{
        for (int i=0;i<threadCount;i++) {
            threadPool.submit(new OrchestratorRunnable());
        }
        boolean success=true;
        try {
            int completedNodes=0;
            while (completedNodes < executions.size() && !stop) {
                try {
                    Entry callable = getCallable();
                    if (null != callable) {
                        //node from orchestrator available, add it to queue, may block until runnable calls take()
                        taskqueue.put(callable);
                    } else if (completedNodes < processedNodes.size()) {
                        //no nodes available, wait for previous tasks to complete
                        Result result = resultqueue.take();
                        if(!result.success){
                            success=false;
                        }
                        if(result.node!=null) {
                            this.orchestrator.returnNode(result.node, result.success, result.result);
                        }
                        completedNodes++;
                    }else if(!orchestrator.isComplete()) {
                        //no nodes available, orchestrator is not complete, perform wait
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }else{
                        break;
                    }
                } catch (DispatcherException e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException e) {

        }finally{
            //attempt to fill the queue to tell waiting threads to stop
            int x=threadCount;
            while (x > 0 && taskqueue.offer(new Entry(true))) {
                x--;
            }
            threadPool.shutdown();
        }
        try {
            threadPool.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
        }

        //stop indicates a node failed, but success might not
        return !stop && success;
    }

    public class OrchestratorRunnable implements Callable<Boolean> {
        @Override
        public Boolean call() throws Exception {
            String originalname = Thread.currentThread().getName();
            while (!stop) {
                boolean success=false;
                Entry task = null;
                NodeStepResult result=null;
                Thread.currentThread().setName("OrchestratorNodeProcessor[take]");
                try {
                    //make sure the get callable happens within the try catch so the finally will always be called
                    task = taskqueue.take();
                    if(task == null || task.finish) {
                        break;
                    }
                    Thread.currentThread().setName(
                            "OrchestratorNodeProcessor[running](node=" +
                            task.node.getNodename() +
                            ")"
                    );
                    result = task.callable.call();
                    success=result.isSuccess();
                    if (!success && !keepgoing) {
                        stop = true;
                        break;
                    }
                } catch (Exception e) {
                    if (!keepgoing) {
                        stop = true;
                        throw e;
                    }
                } finally {
                    resultqueue.put(new Result(task != null ? task.node : null, success, result));
                    Thread.currentThread().setName(originalname);
                }
            }
            return true;
        }
    }

    public Entry getCallable() throws DispatcherException {
        INodeEntry node = this.orchestrator.nextNode();
        if(node == null){
            return null;
        }

        if(!this.processedNodes.add(node)){
            throw new DispatcherException("Can not process the same node twice " + node);
        }

        Callable<NodeStepResult> callable = this.executions.get(node);
        if (null == callable) {
            throw new DispatcherException("Can not process the a node that is not from the target list: " + node);
        }
        return new Entry(node, callable);
    }

    public static class Result {
        private final INodeEntry node;
        private final boolean success;
        private final NodeStepResult result;

        public Result(final INodeEntry node, final boolean success, NodeStepResult result) {
            this.node = node;
            this.success = success;
            this.result=result;
        }

    }
    public static class Entry {
        private boolean finish;
        private final INodeEntry node;
        private final Callable<NodeStepResult> callable;

        public Entry(final boolean finish) {
            this.finish = finish;
            this.node=null;
            this.callable=null;
        }

        public Entry(INodeEntry node, Callable<NodeStepResult> callable) {
            super();
            this.node = node;
            this.callable = callable;
            this.finish=false;
        }

    }

}
