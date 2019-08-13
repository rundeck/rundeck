/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.rules;

import com.dtolabs.rundeck.core.data.DataContext;
import com.dtolabs.rundeck.core.dispatcher.ContextView;
import com.dtolabs.rundeck.core.execution.workflow.WFSharedContext;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.RequiredArgsConstructor;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author greg
 * @since 4/27/17
 */
class WorkflowEngineOperationsProcessor<DAT, RES extends WorkflowSystem.OperationCompleted<DAT>, OP extends
        WorkflowSystem.Operation<DAT, RES>>
{
    private StateWorkflowSystem workflowEngine;
    private WorkflowSystemEventHandler eventHandler;
    private final Set<OP> operations;
    private final WorkflowSystem.SharedData<DAT> sharedData;
    private final Set<WorkflowSystem.Operation> inProcess = Collections.synchronizedSet(new HashSet<>());
    private final Set<WorkflowSystem.Operation> skipped = new HashSet<>();
    private final Set<OP> pending;
    private volatile boolean interrupted;
    private final ListeningExecutorService executorService;
    private final ListeningExecutorService manager;

    private final LinkedBlockingQueue<WorkflowSystem.OperationCompleted<DAT>> stateChangeQueue
            = new LinkedBlockingQueue<>();

    private final Set<WorkflowSystem.OperationResult<DAT, RES, OP>> results
            = Collections.synchronizedSet(new HashSet<WorkflowSystem.OperationResult<DAT, RES, OP>>());

    private final List<ListenableFuture<RES>> futures = new ArrayList<>();

    private WorkflowEngine.Sleeper sleeper = new WorkflowEngine.Sleeper();
    private boolean
            endStateBreak =
            Boolean.parseBoolean(System.getProperty("WorkflowEngineOperationsProcessor.endStateBreak", "false"));
    private boolean
            endStateCancel =
            Boolean.parseBoolean(System.getProperty("WorkflowEngineOperationsProcessor.endStateCancel", "false"));
    private boolean
            endStateCancelInterrupt =
            Boolean.parseBoolean(System.getProperty(
                    "WorkflowEngineOperationsProcessor.endStateCancelInterrupt",
                    "false"
            ));
    ;

    public WorkflowEngineOperationsProcessor(
            final StateWorkflowSystem workflowEngine,
            final WorkflowSystemEventHandler eventHandler,
            final Set<OP> operations,
            final WorkflowSystem.SharedData<DAT> sharedData,
            ListeningExecutorService executorService,
            ListeningExecutorService manager
    )
    {
        this.workflowEngine = workflowEngine;
        this.eventHandler = eventHandler;
        this.operations = operations;
        this.sharedData = sharedData;
        this.pending = new HashSet<>(operations);
        this.executorService = executorService;
        this.manager = manager;
    }

    /**
     * Process the operations from a begin state
     */
    public void beginProcessing() {
        eventHandler.event(WorkflowSystemEventType.Begin, "Workflow begin");
        initialize();
        continueProcessing();
    }


    /**
     * Continue processing from current state
     */
    private void continueProcessing() {
        boolean cancel = false;
        boolean cancelInterrupt = false;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                HashMap<String, String> changes = new HashMap<>();

                //load all changes already in the queue
                getAvailableChanges(changes);

                if (changes.isEmpty()) {
                    if (detectNoMoreChanges()) {
                        //no pending operations, signalling no new state changes will occur
                        eventHandler.event(
                                WorkflowSystemEventType.EndOfChanges,
                                "No more state changes expected, finishing workflow."
                        );
                        return;
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    waitForChanges(changes);
                }
                if (changes.isEmpty()) {
                    //no changes within sleep time, try again
                    continue;
                }

                getContextGlobalData(changes);


                //handle state changes
                processStateChanges(changes);

                if (workflowEngine.isWorkflowEndState()) {
                    eventHandler.event(WorkflowSystemEventType.WorkflowEndState, "Workflow end state reached.");
                    if (endStateBreak) {
                        cancel = endStateCancel;
                        cancelInterrupt = endStateCancelInterrupt;
                        break;
                    } else {
                        return;
                    }
                }
                processOperations(results::add);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (Thread.currentThread().isInterrupted()) {
            eventHandler.event(WorkflowSystemEventType.Interrupted, "Engine interrupted, stopping engine...");
            cancelFutures(true);
            interrupted = Thread.interrupted();
        } else if (cancel) {
            cancelFutures(cancelInterrupt);
        }
        awaitFutures();
    }

    boolean detectNoMoreChanges() {
        return inProcess.isEmpty() && stateChangeQueue.isEmpty();
    }

    /**
     * Poll for changes from the queue, and process all changes that are immediately available without waiting
     * @param changes changes map
     */
    private void getAvailableChanges( final Map<String, String> changes) {
        WorkflowSystem.OperationCompleted<DAT> task = stateChangeQueue.poll();
        while (task != null && task.getNewState() != null) {
            changes.putAll(task.getNewState().getState());
            DAT result = task.getResult();
            if (null != sharedData && null != result) {
                sharedData.addData(result);
            }
            task = stateChangeQueue.poll();
        }
    }

    private void getContextGlobalData(final Map<String, String> changes){
        DAT inputData = null != sharedData ? sharedData.produceNext() : null;
        if(inputData != null && inputData instanceof WFSharedContext) {
            DataContext globals = ((WFSharedContext) inputData).getData().get(ContextView.global());
            if (null != globals) {
                HashMap<String, String> stringStringHashMap = new HashMap<>();
                for (String s : globals.keySet()) {
                    Map<String, String> map = globals.get(s);
                    for (String key : map.keySet()) {

                        if(!workflowEngine.getState().getState().containsKey(s + "." + key) ||
                                !workflowEngine.getState().getState().get(s + "." + key).equals(map.get(key))){
                            stringStringHashMap.put(s + "." + key, map.get(key));
                        }
                    }
                }
                changes.putAll(stringStringHashMap);
            }
        }
    }

    private void awaitFutures() {
        if (!inProcess.isEmpty()) {
            for (ListenableFuture<RES> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException | CancellationException ignored) {

                }
            }
        }
    }

    private void cancelFutures(final boolean mayInterruptIfRunning) {
        futures.forEach(future -> future.cancel(mayInterruptIfRunning));
    }

    public void initialize() {
        //initial state change to start workflow
        stateChangeQueue.add(WorkflowEngine.dummyResult(Workflows.getWorkflowStartState()));
    }

    /**
     * Process the runnable operations
     *
     * @param shouldrun  operations to run
     * @param shouldskip operations to skip
     * @param inputData  input data for the currently runnable operations
     */
    private void processRunnableOperations(
            final Consumer<WorkflowSystem.OperationResult<DAT, RES, OP>> resultConsumer,
            final List<OP> shouldrun,
            final List<OP> shouldskip,
            final DAT inputData
    )
    {
        for (final OP operation : shouldrun) {
            if (shouldskip.contains(operation)) {
                continue;
            }
            pending.remove(operation);

            eventHandler.event(
                    WorkflowSystemEventType.WillRunOperation,
                    String.format("operation starting: %s", operation),
                    operation
            );
            final ListenableFuture<RES> submit = beginOperation(inputData, operation);
            FutureCallback<RES> callback = new OperationFutureCallback(eventHandler, operation, resultConsumer);
            Futures.addCallback(submit, callback, manager);
        }
    }

    @RequiredArgsConstructor
    class OperationFutureCallback
            implements FutureCallback<RES>
    {
        final WorkflowSystemEventHandler eventHandler;
        final OP operation;
        final Consumer<WorkflowSystem.OperationResult<DAT, RES, OP>> resultConsumer;

        @Override
        public void onSuccess(final RES successResult) {
            eventHandler.event(
                    WorkflowSystemEventType.OperationSuccess,
                    String.format("operation succeeded: %s", successResult),
                    successResult
            );
            assert successResult != null;
            WorkflowSystem.OperationResult<DAT, RES, OP> result = result(successResult, operation);
            resultConsumer.accept(result);
            finishedOperation(successResult, operation);
        }

        @Override
        public void onFailure(final Throwable t) {
            eventHandler.event(
                    WorkflowSystemEventType.OperationFailed,
                    String.format("operation failed: %s", t),
                    t
            );
            WorkflowSystem.OperationResult<DAT, RES, OP> result = result(t, operation);
            resultConsumer.accept(result);
            StateObj newFailureState = operation.getFailureState(t);
            if (null == newFailureState) {
                newFailureState = new DataState();
            }
            finishedOperation(WorkflowEngine.<DAT>dummyResult(newFailureState), operation);
        }
    }

    private ListenableFuture<RES> beginOperation(final DAT inputData, final OP operation) {
        final ListenableFuture<RES> submit = executorService.submit(() -> operation.apply(inputData));
        inProcess.add(operation);
        futures.add(submit);
        return submit;
    }

    private void finishedOperation(final WorkflowSystem.OperationCompleted<DAT> e, final OP operation) {
        stateChangeQueue.add(e);
        inProcess.remove(operation);
    }

    private <D, T extends WorkflowSystem.OperationCompleted<D>, X extends WorkflowSystem.Operation<D, T>>
    WorkflowSystem.OperationResult<D, T, X> result(
            final Throwable t, final X operation
    )
    {
        return new WorkflowEngine.WResult<>(operation, t);
    }

    private <D, T extends WorkflowSystem.OperationCompleted<D>, X extends WorkflowSystem.Operation<D, T>>
    WorkflowSystem.OperationResult<D, T, X> result(
            final T successResult, final X operation
    )
    {
        return new WorkflowEngine.WResult<>(operation, successResult);
    }

    /**
     * Sleep until changes are available on the queue, if any are found then consume remaining and
     * return false, otherwise return true
     *
     * @param changes
     *
     * @return true if no changes found in the sleep time.
     *
     * @throws InterruptedException
     */
    private void waitForChanges(final Map<String, String> changes) throws InterruptedException {
        WorkflowSystem.OperationCompleted<DAT> take = stateChangeQueue.poll(sleeper.time(), sleeper.unit());
        if (null == take || take.getNewState().getState().isEmpty()) {
            sleeper.backoff();
            return;
        }

        sleeper.reset();

        changes.putAll(take.getNewState().getState());

        if (null != sharedData) {
            sharedData.addData(take.getResult());
        }

        getAvailableChanges(changes);
    }

    /**
     * Handle the state changes for the rule engine
     *
     * @param changes
     */
    private void processStateChanges(final Map<String, String> changes) {
        eventHandler.event(WorkflowSystemEventType.WillProcessStateChange,
                           String.format("saw state changes: %s", changes), changes
        );

        workflowEngine.getState().updateState(changes);

        boolean update = Rules.update(workflowEngine.getRuleEngine(), workflowEngine.getState());
        eventHandler.event(
                WorkflowSystemEventType.DidProcessStateChange,
                String.format(
                        "applied state changes and rules (changed? %s): %s",
                        update,
                        workflowEngine.getState()
                ),
                workflowEngine.getState()
        );
    }

    /**
     * Run and skip pending operations
     *
     * @param resultConsumer consumer for result of operations
     */
    private void processOperations(
            final Consumer<WorkflowSystem.OperationResult<DAT, RES, OP>> resultConsumer
    )
    {
        int origPendingCount = pending.size();

        //operations which match runnable conditions
        List<OP> shouldrun = pending.stream()
                                    .filter(input -> input.shouldRun(workflowEngine.getState()))
                                    .collect(Collectors.toList());

        //runnable that should be skipped
        List<OP> shouldskip = shouldrun.stream()
                                       .filter(input -> input.shouldSkip(workflowEngine.getState()))
                                       .collect(Collectors.toList());

        //shared data
        DAT inputData = null != sharedData ? sharedData.produceNext() : null;


        processRunnableOperations(resultConsumer, shouldrun, shouldskip, inputData);

        processSkippedOperations(shouldskip);

        pending.removeAll(shouldskip);
        skipped.addAll(shouldskip);

        eventHandler.event(
                WorkflowSystemEventType.LoopProgress,
                String.format(
                        "Pending(%d) => run(%d), skip(%d), remain(%d)",
                        origPendingCount,
                        shouldrun.size() - shouldskip.size(),
                        shouldskip.size(),
                        pending.size()
                )
        );
    }

    /**
     * Process skipped operations
     *
     * @param shouldskip list of operations to skip
     */
    private void processSkippedOperations(final List<OP> shouldskip) {
        for (final OP operation : shouldskip) {
            eventHandler.event(
                    WorkflowSystemEventType.WillSkipOperation,
                    String.format("Skip condition statisfied for operation: %s, skipping", operation),
                    operation
            );
            StateObj newstate = operation.getSkipState(workflowEngine.getState());
            WorkflowSystem.OperationCompleted<DAT> objectOperationCompleted = WorkflowEngine.dummyResult(newstate);
            stateChangeQueue.add(objectOperationCompleted);
        }
    }

    public Set<OP> getOperations() {
        return operations;
    }

    public WorkflowSystem.SharedData<DAT> getSharedData() {
        return sharedData;
    }

    public Set<WorkflowSystem.Operation> getInProcess() {
        return inProcess;
    }

    public Set<WorkflowSystem.Operation> getSkipped() {
        return skipped;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }

    public Set<OP> getPending() {
        return pending;
    }

    public Set<WorkflowSystem.OperationResult<DAT, RES, OP>> getResults() {
        return results;
    }
}
