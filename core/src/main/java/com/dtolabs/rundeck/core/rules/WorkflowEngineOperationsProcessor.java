/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

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
    private WorkflowEngine workflowEngine;
    private final Set<OP> operations;
    private final WorkflowSystem.SharedData<DAT> sharedData;
    private final Set<WorkflowSystem.Operation> inProcess = Collections.synchronizedSet(new HashSet<WorkflowSystem
            .Operation>());
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

    public WorkflowEngineOperationsProcessor(
            final WorkflowEngine workflowEngine,
            final Set<OP> operations,
            final WorkflowSystem.SharedData<DAT> sharedData,
            ListeningExecutorService executorService,
            ListeningExecutorService manager
    )
    {
        this.workflowEngine = workflowEngine;
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
        workflowEngine.event(WorkflowSystemEventType.Begin, "Workflow begin");
        initialize();
        continueProcessing();
    }


    /**
     * Continue processing from current state
     */
    private void continueProcessing() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                HashMap<String, String> changes = new HashMap<>();

                //load all changes already in the queue
                getAvailableChanges(changes);

                if (changes.isEmpty()) {
                    if (inProcess.isEmpty()) {
                        //no pending operations, signalling no new state changes will occur
                        workflowEngine.event(
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

                //handle state changes
                processStateChanges(changes);

                if (workflowEngine.isWorkflowEndState(workflowEngine.getState())) {
                    workflowEngine.event(WorkflowSystemEventType.WorkflowEndState, "Workflow end state reached.");
                    return;
                }
                processOperations(results::add);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (Thread.currentThread().isInterrupted()) {
            workflowEngine.event(WorkflowSystemEventType.Interrupted, "Engine interrupted, stopping engine...");
            cancelFutures();
            interrupted = Thread.interrupted();
        }
        awaitFutures();
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

    private void cancelFutures() {
        for (ListenableFuture<RES> future : futures) {
            future.cancel(true);
        }
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

            workflowEngine.event(
                    WorkflowSystemEventType.WillRunOperation,
                    String.format("operation starting: %s", operation),
                    operation
            );
            final ListenableFuture<RES> submit = executorService.submit(() -> operation.apply(inputData));
            inProcess.add(operation);
            futures.add(submit);
            FutureCallback<RES> callback = new FutureCallback<RES>() {
                @Override
                public void onSuccess(final RES successResult) {
                    workflowEngine.event(
                            WorkflowSystemEventType.OperationSuccess,
                            String.format("operation succeeded: %s", successResult),
                            successResult
                    );
                    assert successResult != null;
                    WorkflowSystem.OperationResult<DAT, RES, OP> result = result(successResult, operation);
                    resultConsumer.accept(result);
                    stateChangeQueue.add(successResult);
                    inProcess.remove(operation);
                }

                @Override
                public void onFailure(final Throwable t) {
                    workflowEngine.event(
                            WorkflowSystemEventType.OperationFailed,
                            String.format("operation failed: %s", t),
                            t
                    );
                    WorkflowSystem.OperationResult<DAT, RES, OP> result = result(t, operation);
                    resultConsumer.accept(result);
                    StateObj newFailureState = operation.getFailureState(t);
                    if (null != newFailureState && newFailureState.getState().size() > 0) {
                        WorkflowSystem.OperationCompleted<DAT> objectOperationCompleted = WorkflowEngine.dummyResult(
                                newFailureState);
                        stateChangeQueue.add(objectOperationCompleted);
                    }
                    inProcess.remove(operation);
                }
            };

            Futures.addCallback(submit, callback, manager);
        }
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
        workflowEngine.event(WorkflowSystemEventType.WillProcessStateChange,
                             String.format("saw state changes: %s", changes), changes
        );

        workflowEngine.getState().updateState(changes);

        boolean update = Rules.update(workflowEngine.getRuleEngine(), workflowEngine.getState());
        workflowEngine.event(
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

        workflowEngine.eventLoopProgress(origPendingCount, shouldskip.size(), shouldrun.size(), pending.size());
    }

    /**
     * Process skipped operations
     *
     * @param shouldskip list of operations to skip
     */
    private void processSkippedOperations(final List<OP> shouldskip) {
        for (final OP operation : shouldskip) {
            workflowEngine.event(
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
