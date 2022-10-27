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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.Getter;
import lombok.Setter;

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
    @Getter private final Set<OP> operations;
    @Getter private final WorkflowSystem.SharedData<DAT, Map<String, String>> sharedData;
    @Getter private final Set<WorkflowSystem.Operation>
            inProcess =
            Collections.synchronizedSet(new HashSet<>());
    @Getter private final Set<WorkflowSystem.Operation> skipped = new HashSet<>();
    @Getter private final Set<OP> pending;
    @Setter
    @Getter
    private volatile boolean interrupted;
    @Setter @Getter private StateObj initialState = Workflows.getWorkflowStartState();

    private final ListeningExecutorService executorService;
    private final ListeningExecutorService manager;

    private final LinkedBlockingQueue<WorkflowSystem.OperationCompleted<DAT>> stateChangeQueue
            = new LinkedBlockingQueue<>();

    @Getter private final Set<WorkflowSystem.OperationResult<DAT, RES, OP>> results
            = Collections.synchronizedSet(new HashSet<>());

    private final List<ListenableFuture<RES>> futures = new ArrayList<>();

    private WorkflowEngine.Sleeper sleeper = new WorkflowEngine.Sleeper();


    public WorkflowEngineOperationsProcessor(
            final StateWorkflowSystem workflowEngine,
            final WorkflowSystemEventHandler eventHandler,
            final Set<OP> operations,
            final WorkflowSystem.SharedData<DAT, Map<String, String>> sharedData,
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
        eventHandler.event(
                WorkflowSystemEventType.Begin,
                "Workflow begin",
                StateWorkflowSystem.stateEvent(workflowEngine.getState(), sharedData)
        );
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
                //wait for changes
                boolean changed = processCompletedChanges(waitForChanges());

                if (!changed) {
                    if (detectNoMoreChanges()) {
                        //no pending operations, signalling no new state changes will occur
                        eventHandler.event(
                                WorkflowSystemEventType.EndOfChanges,
                                "No more state changes expected, finishing workflow.",
                                StateWorkflowSystem.stateEvent(workflowEngine.getState(), sharedData)
                        );
                        return;
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    //no changes within sleep time, try again
                    continue;
                }


                if (shouldWorkflowEnd()) {
                    //end state reached, and either; gather was true and no more changes are pending, or gather was not true
                    eventHandler.event(
                            WorkflowSystemEventType.WorkflowEndState,
                            "Workflow end state reached.",
                            StateWorkflowSystem.stateEvent(workflowEngine.getState(), sharedData)
                    );

                    return;
                }else if(!workflowEngine.isWorkflowEndState()){
                    //some changes made to state, so review pending operations
                    processOperations(results::add);
                }
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

    boolean shouldWorkflowEnd() {
        return workflowEngine.isWorkflowEndState() && detectNoMoreChanges();
    }

    private boolean processCompletedChanges(
            final List<WorkflowSystem.OperationCompleted<DAT>> operationCompleteds
    )
    {
        if (operationCompleteds == null) {
            return false;
        }
        boolean changed = false;
        for (WorkflowSystem.OperationCompleted<DAT> task : operationCompleteds) {
            changed |=
                    workflowEngine.processStateChange(
                            task.getIdentity(),
                            task.getNewState(),
                            task.getResult(),
                            sharedData
                    );
        }
        return changed;
    }

    /**
     * @return true, if no running operations and no queued changes
     */
    synchronized boolean detectNoMoreChanges() {
        //nb: synchronized to avoid race with finishedOperation method
        return inProcess.isEmpty() && stateChangeQueue.isEmpty();
    }


    private List<WorkflowSystem.OperationCompleted<DAT>> pollChanges() {
        List<WorkflowSystem.OperationCompleted<DAT>> changes = new ArrayList<>();
        WorkflowSystem.OperationCompleted<DAT> task = stateChangeQueue.poll();
        while (task != null && task.getNewState() != null) {
            changes.add(task);
            task = stateChangeQueue.poll();
        }
        return changes;
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
        stateChangeQueue.add(WorkflowEngine.dummyResult(initialState, "init", true));
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
                    StateWorkflowSystem.operationEvent(operation.getIdentity(), workflowEngine.getState(), sharedData)
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
            WorkflowSystem.OperationResult<DAT, RES, OP> result = result(successResult, operation);
            eventHandler.event(
                    result.getSuccess().isSuccess()
                    ? WorkflowSystemEventType.OperationSuccess
                    : WorkflowSystemEventType.OperationFailed,
                    String.format(
                            "operation completed, success? %s: %s",
                            result.getSuccess().isSuccess(),
                            successResult
                    ),
                    StateWorkflowSystem.operationCompleteEvent(
                            operation.getIdentity(),
                            workflowEngine.getState(),
                            sharedData,
                            result
                    )
            );
            assert successResult != null;
            resultConsumer.accept(result);
            finishedOperation(successResult, operation);
        }

        @Override
        public void onFailure(final Throwable t) {
            WorkflowSystem.OperationResult<DAT, RES, OP> result = result(t, operation);
            try{
                eventHandler.event(
                        WorkflowSystemEventType.OperationFailed,
                        String.format("operation failed: %s", t),
                        StateWorkflowSystem.operationCompleteEvent(
                                operation.getIdentity(),
                                workflowEngine.getState(),
                                sharedData,
                                result
                        )
                );
            } finally {
                resultConsumer.accept(result);
                StateObj newFailureState = operation.getFailureState(t);
                if (null == newFailureState) {
                    newFailureState = new DataState();
                }
                finishedOperation(WorkflowEngine.<DAT>dummyResult(newFailureState, operation.getIdentity(), false), operation);
            }
        }
    }

    private ListenableFuture<RES> beginOperation(final DAT inputData, final OP operation) {
        final ListenableFuture<RES> submit = executorService.submit(() -> operation.apply(inputData));
        inProcess.add(operation);
        futures.add(submit);
        return submit;
    }

    private synchronized void finishedOperation(final WorkflowSystem.OperationCompleted<DAT> e, final OP operation) {
        //synchronize to prevent race condition with detectNoMoreChanges method
        //because this method is called by an operation's thread, and the
        //main thread may process the state change entry prior to
        //this method removing the operation from inProcess set
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
     * Sleep for some time until changes are available on the queue, if any are found then consume remaining return all
     * results
     *
     * @return list of
     * @throws InterruptedException during sleep
     */
    private List<WorkflowSystem.OperationCompleted<DAT>> waitForChanges() throws InterruptedException {
        List<WorkflowSystem.OperationCompleted<DAT>> results = new ArrayList<>();
        WorkflowSystem.OperationCompleted<DAT> take = stateChangeQueue.poll(sleeper.time(), sleeper.unit());
        if (null == take || take.getNewState().getState().isEmpty()) {
            sleeper.backoff();
            return results;
        } else {
            sleeper.reset();
        }

        results.add(take);
        results.addAll(pollChanges());
        return results;
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
                    StateWorkflowSystem.operationEvent(operation.getIdentity(), workflowEngine.getState(), sharedData)
            );
            StateObj newstate = operation.getSkipState(workflowEngine.getState());
            WorkflowSystem.OperationCompleted<DAT> objectOperationCompleted = WorkflowEngine.dummyResult(
                    newstate,
                    operation.getIdentity(),
                    true
            );
            stateChangeQueue.add(objectOperationCompleted);

            pending.remove(operation);
            skipped.add(operation);
        }
    }

}
