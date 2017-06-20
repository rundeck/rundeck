package com.dtolabs.rundeck.core.rules;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.util.concurrent.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A WorkflowSystem which processes the operations by use of a rule system and a mutable state.
 * implementation: Operations can supply state changes after they succeed, or fail. State
 * changes
 * provided by
 * operations are added in the order they are received, and after a set of available state
 * changes are added, the rule engine is used to update the state based on its rules.  After
 * the state is updated, all pending operations are queried to see if they can run given the new
 * state, and any that can are queued to be executed.  Workflow processing stops when no operations
 * are currently running, no new state changes are available, and no pending operations can be run.
 */
public class WorkflowEngine implements WorkflowSystem {
    static Logger logger = Logger.getLogger(WorkflowEngine.class.getName());
    private MutableStateObj state;
    private final RuleEngine engine;
    private final LinkedBlockingQueue<StateObj> stateChangeQueue;
    private final Set<Operation> inProcess = Collections.synchronizedSet(new HashSet<Operation>());
    private final Set<Operation> skipped = new HashSet<>();
    private final ListeningExecutorService executorService;
    private final ListeningExecutorService manager;

    private WorkflowSystemEventListener listener;
    private volatile boolean interrupted;

    /**
     * Create engine
     *
     * @param engine   rule engine to process state changes via rules
     * @param state    initial state
     * @param executor executor to process operations, which should be multithreaded to process operations concurrently
     */
    public WorkflowEngine(
            final RuleEngine engine,
            final MutableStateObj state,
            final ExecutorService executor

    )
    {
        this.engine = engine;
        this.state = state;
        stateChangeQueue = new LinkedBlockingQueue<>();
        executorService = MoreExecutors.listeningDecorator(executor);
        manager = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    }

    public <T extends OperationSuccess, X extends Operation<T>> Set<OperationResult<T, X>> processOperations(
            final Set<X> operations
    )
    {
        event(WorkflowSystemEventType.Begin, "Workflow begin");
        //initial state change to start workflow
        queueChange(Workflows.getWorkflowStartState());
        final Set<X> pending = new HashSet<>(operations);
        final Set<OperationResult<T, X>> results = Collections.synchronizedSet(new HashSet<OperationResult<T, X>>());
        final List<ListenableFuture<T>> futures = Collections.synchronizedList(new ArrayList<ListenableFuture<T>>());
        interrupted = false;
        long sleepOrig = 250;
        long sleepMult = 2;
        long sleepmax = 1000*5;
        long sleeptime = sleepOrig;
        while (!interrupted) {
            try {
                HashMap<String, String> changes = new HashMap<>();
                //load all changes already in the queue
                pollAll(changes);
                if (changes.size() < 1) {
                    //no changes seen, so wait for any change
                    if (inProcess.size() < 1) {
                        //no pending operations, signalling no new state changes will occur
                        event(
                                WorkflowSystemEventType.EndOfChanges,
                                "No more state changes expected, finishing workflow."
                        );
                        break;
                    }
                    //otherwise wait for change
                    StateObj take = stateChangeQueue.poll(sleeptime, TimeUnit.MILLISECONDS);
                    if (null == take || take.getState().size() < 1) {
                        sleeptime = Math.min(sleeptime * sleepMult, sleepmax);
                        continue;
                    }
                    sleeptime = sleepOrig;
                    changes.putAll(take.getState());
                    pollAll(changes);
                }
                event(WorkflowSystemEventType.WillProcessStateChange,
                      String.format("saw state changes: %s", changes), changes
                );

                state.updateState(changes);

                boolean update = Rules.update(engine, state);
                event(WorkflowSystemEventType.DidProcessStateChange,
                      String.format("applied state changes and rules (changed? %s): %s", update, state), state
                );


                if (isWorkflowEndState(state)) {
                    event(WorkflowSystemEventType.WorkflowEndState, "Workflow end state reached.");
                    break;
                }

                int origPendingCount = pending.size();

                //operations which match runnable conditions
                FluentIterable<X> runnable = FluentIterable.from(pending).filter(shouldRun(state));

                //runnable that should be skipped
                List<X> shouldskip = runnable.filter(shouldSkip(state, true)).toList();

                //runnable, should not skip
                List<X> shouldrun = runnable.toList();

                for (final X operation : shouldrun) {
                    if (shouldskip.contains(operation)) {
                        continue;
                    }
                    pending.remove(operation);

                    event(
                            WorkflowSystemEventType.WillRunOperation,
                            String.format("operation starting: %s", operation),
                            operation
                    );
                    final ListenableFuture<T> submit = executorService.submit(operation);
                    inProcess.add(operation);
                    futures.add(submit);
                    FutureCallback<T> cleanup = new FutureCallback<T>() {
                        @Override
                        public void onSuccess(final T result) {
                            futures.remove(submit);
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            futures.remove(submit);
                        }
                    };
                    FutureCallback<T> callback = new FutureCallback<T>() {
                        @Override
                        public void onSuccess(final T successResult) {
                            event(
                                    WorkflowSystemEventType.OperationSuccess,
                                    String.format("operation succeeded: %s", successResult),
                                    successResult
                            );
                            assert successResult != null;
                            OperationResult<T, X> result = result(successResult, operation);
                            synchronized (results) {
                                results.add(result);
                            }
                            queueChange(successResult.getNewState());
                            inProcess.remove(operation);
                        }

                        @Override
                        public void onFailure(final Throwable t) {
                            event(
                                    WorkflowSystemEventType.OperationFailed,
                                    String.format("operation failed: %s", t),
                                    t
                            );
                            OperationResult<T, X> result = result(t, operation);
                            synchronized (results) {
                                results.add(result);
                            }
                            StateObj newFailureState = operation.getFailureState(t);
                            if (null != newFailureState && newFailureState.getState().size() > 0) {
                                queueChange(newFailureState);
                            }
                            inProcess.remove(operation);

                        }
                    };
                    Futures.addCallback(submit, callback, manager);
                    Futures.addCallback(submit, cleanup, manager);
                }
                for (final X operation : shouldskip) {
                    event(
                            WorkflowSystemEventType.WillSkipOperation,
                            String.format("Skip condition statisfied for operation: %s, skipping", operation),
                            operation
                    );
                    pending.remove(operation);
                    skipped.add(operation);
                    StateObj newstate = operation.getSkipState(state);
                    queueChange(newstate);
                }

                event(
                        WorkflowSystemEventType.LoopProgress,
                        String.format(
                                "Pending(%d) => run(%d), skip(%d), remain(%d)",
                                origPendingCount,
                                shouldrun.size() - shouldskip.size(),
                                shouldskip.size(),
                                pending.size()
                        )
                );
            } catch (InterruptedException e) {
                interrupted = true;
                break;
            }
        }
        if (interrupted) {
            event(WorkflowSystemEventType.Interrupted, "Engine interrupted, stopping engine...");
            //attempt to cancel all futures
            synchronized (futures) {
                for (ListenableFuture<T> future : futures) {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                }
            }
        }

        event(WorkflowSystemEventType.WillShutdown, "Workflow engine shutting down");

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ignored) {
        }

        if (inProcess.size() > 0) {
            //attempt to wait for cancel results to be received
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {

            }
        }

        manager.shutdown();
        try {
            if (!manager.awaitTermination(5, TimeUnit.MINUTES)) {
                manager.shutdownNow();
            }
        } catch (InterruptedException ignored) {
        }
        if (pending.size() > 0) {

            event(
                    WorkflowSystemEventType.IncompleteOperations,
                    String.format("Some operations were not run: %d", pending.size()),
                    pending
            );
        }
        event(WorkflowSystemEventType.Complete, String.format("Workflow complete: %s", results));
        return results;
    }

    private void event(final WorkflowSystemEventType eventType, final String message) {
        event(eventType, message, null);
    }

    private void event(final WorkflowSystemEventType eventType, final String message, final Object data) {
        logDebug(message);

        if (null != listener) {
            listener.onEvent(new WorkflowSystemEvent() {
                @Override
                public String getMessage() {
                    return message;
                }

                @Override
                public WorkflowSystemEventType getEventType() {
                    return eventType;
                }

                @Override
                public Object getData() {
                    return data;
                }
            });
        }
    }

    private void logDebug(final String message) {
        logger.debug(message);
    }

    protected boolean isWorkflowEndState(final MutableStateObj state) {
        return state.hasState(Workflows.getWorkflowEndState());
    }

    public WorkflowSystemEventListener getListener() {
        return listener;
    }

    public void setListener(WorkflowSystemEventListener listener) {
        this.listener = listener;
    }

    public boolean isInterrupted() {
        return interrupted;
    }

    public void setInterrupted(boolean interrupted) {
        this.interrupted = interrupted;
    }


    private static class WResult<T extends OperationSuccess, X extends Operation<T>> implements OperationResult<T, X> {
        final private X operation;

        WResult(final X operation, final Throwable throwable) {
            this.operation = operation;
            this.throwable = throwable;
            this.success = null;
        }

        WResult(final X operation, final T success) {
            this.operation = operation;
            this.success = success;
            this.throwable = null;
        }

        final Throwable throwable;
        final T success;

        @Override
        public Throwable getFailure() {
            return throwable;
        }

        @Override
        public T getSuccess() {
            return success;
        }

        @Override
        public X getOperation() {
            return operation;
        }
    }

    private <T extends OperationSuccess, X extends Operation<T>> OperationResult<T, X> result(
            final Throwable t, final X operation
    )
    {
        return new WResult<>(operation, t);
    }

    private <T extends OperationSuccess, X extends Operation<T>> OperationResult<T, X> result(
            final T successResult, final X operation
    )
    {
        return new WResult<>(operation, successResult);
    }


    static private Predicate<Operation> shouldRun(final StateObj state) {
        return new Predicate<Operation>() {
            @Override
            public boolean apply(final Operation input) {
                assert input != null;
                return input.shouldRun(state);
            }
        };
    }

    static private Predicate<Operation> shouldSkip(final StateObj state, final boolean requireSkip) {
        return new Predicate<Operation>() {
            @Override
            public boolean apply(final Operation input) {
                assert input != null;
                return requireSkip == input.shouldSkip(state);
            }
        };
    }


    private void queueChange(StateObj state) {
        stateChangeQueue.add(state);
    }


    private Map<String, String> map(final String key, final String value) {
        HashMap<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    private void pollAll(final HashMap<String, String> changes) {
        StateObj task = stateChangeQueue.poll();
        while (task != null) {
            changes.putAll(task.getState());
            task = stateChangeQueue.poll();
        }
    }
}
