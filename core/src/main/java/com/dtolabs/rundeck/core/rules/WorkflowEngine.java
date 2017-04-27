package com.dtolabs.rundeck.core.rules;

import com.google.common.util.concurrent.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

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
        executorService = MoreExecutors.listeningDecorator(executor);
        manager = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    }

    class Sleeper {
        private long orig = 250;
        private long mult = 2;
        private long max = 5000L;
        private long time = orig;
        private TimeUnit unit = TimeUnit.MILLISECONDS;

        long time() {
            return time;
        }

        void backoff() {
            time = Math.min(time * mult, max);
        }

        void reset() {
            time = orig;
        }

        TimeUnit unit() {
            return unit;
        }
    }

    public <DAT,
            RES extends OperationSuccess<DAT>,
            OP extends Operation<DAT, RES>
            >
    Set<OperationResult<DAT, RES, OP>> processOperations(final Set<OP> operations, final SharedData<DAT> sharedData) {

        event(WorkflowSystemEventType.Begin, "Workflow begin");
        final LinkedBlockingQueue<OperationSuccess<DAT>> stateChangeQueue = new LinkedBlockingQueue<>();
        //initial state change to start workflow
        OperationSuccess<DAT> begin = WorkflowEngine.dummyResult(Workflows.getWorkflowStartState());
        stateChangeQueue.add(begin);
        final Set<OP> pending = new HashSet<>(operations);
        final Set<OperationResult<DAT, RES, OP>> results = Collections.synchronizedSet(
                new HashSet<OperationResult<DAT, RES, OP>>()
        );
        final List<ListenableFuture<RES>> futures = Collections.synchronizedList(new ArrayList<ListenableFuture<RES>>
                                                                                         ());
        interrupted = false;
        Sleeper sleeper = new Sleeper();
        try {
            while (!interrupted) {
                HashMap<String, String> changes = new HashMap<>();

                //load all changes already in the queue
                pollAllChanges(stateChangeQueue, changes, sharedData);

                if (changes.isEmpty() && inProcess.isEmpty()) {
                    //no pending operations, signalling no new state changes will occur
                    event(
                            WorkflowSystemEventType.EndOfChanges,
                            "No more state changes expected, finishing workflow."
                    );
                    break;
                }

                //wait for any changes
                if (changes.isEmpty() && waitForChangeHasNoResults(sharedData, stateChangeQueue, sleeper, changes)) {
                    //no changes are found, repeat loop and wait again
                    continue;
                }

                if (processChanges(sharedData, stateChangeQueue, pending, results, futures, changes)) {
                    event(WorkflowSystemEventType.WorkflowEndState, "Workflow end state reached.");
                    break;
                }

            }
        } catch (InterruptedException e) {
            interrupted = true;
            Thread.currentThread().interrupt();
        }
        if (interrupted) {
            event(WorkflowSystemEventType.Interrupted, "Engine interrupted, stopping engine...");
            //attempt to cancel all futures
            synchronized (futures) {
                for (ListenableFuture<RES> future : futures) {
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
            Thread.currentThread().interrupt();
        }

        if (!inProcess.isEmpty()) {
            //attempt to wait for cancel results to be received
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();

            }
        }

        manager.shutdown();
        try {
            if (!manager.awaitTermination(5, TimeUnit.MINUTES)) {
                manager.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (!pending.isEmpty()) {
            event(
                    WorkflowSystemEventType.IncompleteOperations,
                    String.format("Some operations were not run: %d", pending.size()),
                    pending
            );
        }
        event(WorkflowSystemEventType.Complete, String.format("Workflow complete: %s", results));
        return results;
    }

    /**
     * Process the state changes and 
     * @param sharedData
     * @param stateChangeQueue
     * @param pending
     * @param results
     * @param futures
     * @param changes
     * @param <DAT>
     * @param <RES>
     * @param <OP>
     * @return
     */
    private  <DAT, RES extends OperationSuccess<DAT>, OP extends Operation<DAT, RES>> boolean processChanges(
            final SharedData<DAT> sharedData,
            final LinkedBlockingQueue<OperationSuccess<DAT>> stateChangeQueue,
            final Set<OP> pending,
            final Set<OperationResult<DAT, RES, OP>> results,
            final List<ListenableFuture<RES>> futures,
            final Map<String, String> changes
    )
    {
        event(WorkflowSystemEventType.WillProcessStateChange,
              String.format("saw state changes: %s", changes), changes
        );

        state.updateState(changes);

        boolean update = Rules.update(engine, state);
        event(WorkflowSystemEventType.DidProcessStateChange,
              String.format("applied state changes and rules (changed? %s): %s", update, state), state
        );


        if (isWorkflowEndState(state)) {
            return true;
        }

        int origPendingCount = pending.size();

        //operations which match runnable conditions
        List<OP> shouldrun = pending.stream()
                                    .filter(input -> input.shouldRun(state))
                                    .collect(Collectors.toList());

        //runnable that should be skipped
        List<OP> shouldskip = shouldrun.stream()
                                       .filter(input -> input.shouldSkip(state))
                                       .collect(Collectors.toList());

        //shared data
        DAT inputData = null != sharedData ? sharedData.produceNext() : null;


        processRunnableOperations(
                stateChangeQueue,
                pending,
                results,
                futures,
                shouldrun,
                shouldskip,
                inputData
        );

        processSkippedOperations(stateChangeQueue, pending, shouldskip);

        eventLoopProgress(origPendingCount, shouldskip.size(), shouldrun.size(), pending.size());
        return false;
    }

    private void eventLoopProgress(
            final int origPendingCount,
            final int skipcount,
            final int toruncount,
            final int pendingcount
    )
    {
        event(
                WorkflowSystemEventType.LoopProgress,
                String.format(
                        "Pending(%d) => run(%d), skip(%d), remain(%d)",
                        origPendingCount,
                        toruncount - skipcount,
                        skipcount,
                        pendingcount
                )
        );
    }

    /**
     * Process skipped operations
     *
     * @param stateChangeQueue queue
     * @param pending          pending operations
     * @param shouldskip       list of operations to skip
     * @param <DAT>            data type
     * @param <RES>            result type
     * @param <OP>             operation type
     */
    private <DAT, RES extends OperationSuccess<DAT>, OP extends Operation<DAT, RES>> void processSkippedOperations(
            final LinkedBlockingQueue<OperationSuccess<DAT>> stateChangeQueue,
            final Set<OP> pending,
            final List<OP> shouldskip
    )
    {
        for (final OP operation : shouldskip) {
            event(
                    WorkflowSystemEventType.WillSkipOperation,
                    String.format("Skip condition statisfied for operation: %s, skipping", operation),
                    operation
            );
            pending.remove(operation);
            skipped.add(operation);
            StateObj newstate = operation.getSkipState(state);
            OperationSuccess<DAT> objectOperationSuccess = dummyResult(newstate);
            stateChangeQueue.add(objectOperationSuccess);
        }
    }

    /**
     * Sleep until changes are available on the queue, if any are found then consume remaining and
     * return false, otherwise return true
     *
     * @param sharedData
     * @param stateChangeQueue
     * @param sleeper
     * @param changes
     * @param <DAT>
     *
     * @return true if no changes found in the sleep time.
     *
     * @throws InterruptedException
     */
    private <DAT> boolean waitForChangeHasNoResults(
            final SharedData<DAT> sharedData,
            final LinkedBlockingQueue<OperationSuccess<DAT>> stateChangeQueue,
            final WorkflowEngine.Sleeper sleeper,
            final Map<String, String> changes
    ) throws InterruptedException
    {
        OperationSuccess<DAT> take = stateChangeQueue.poll(sleeper.time(), sleeper.unit());
        if (null == take || take.getNewState().getState().isEmpty()) {
            sleeper.backoff();
            return true;
        }

        sleeper.reset();

        changes.putAll(take.getNewState().getState());

        if (null != sharedData) {
            sharedData.addData(take.getResult());
        }

        pollAllChanges(stateChangeQueue, changes, sharedData);

        return false;
    }

    static class Event implements WorkflowSystemEvent {
        private WorkflowSystemEventType eventType;
        private String message;
        private Object data;

        Event(final WorkflowSystemEventType eventType, final String message, final Object data) {
            this.eventType = eventType;
            this.message = message;
            this.data = data;
        }

        static Event with(WorkflowSystemEventType type, String message) {
            return new Event(type, message, null);
        }

        static Event with(WorkflowSystemEventType type, String message, Object data) {
            return new Event(type, message, data);
        }

        @Override
        public WorkflowSystemEventType getEventType() {
            return eventType;
        }

        public void setEventType(WorkflowSystemEventType eventType) {
            this.eventType = eventType;
        }

        @Override
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @Override
        public Object getData() {
            return data;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }

    /**
     * Process the runnable operations
     *
     * @param stateChangeQueue queue
     * @param pending          pending operations
     * @param results          result set
     * @param futures          futures
     * @param shouldrun        operations to run
     * @param shouldskip       operations to skip
     * @param inputData        input data for the currently runnable operations
     * @param <DAT>            data type
     * @param <RES>            result type
     * @param <OP>             operation type
     */
    private <DAT, RES extends OperationSuccess<DAT>, OP extends Operation<DAT, RES>> void processRunnableOperations(
            final LinkedBlockingQueue<OperationSuccess<DAT>> stateChangeQueue,
            final Set<OP> pending,
            final Set<OperationResult<DAT, RES, OP>> results,
            final List<ListenableFuture<RES>> futures,
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

            event(
                    WorkflowSystemEventType.WillRunOperation,
                    String.format("operation starting: %s", operation),
                    operation
            );
            final ListenableFuture<RES> submit = executorService.submit(operationCallable(operation, inputData));
            synchronized (inProcess) {
                inProcess.add(operation);
            }
            futures.add(submit);
            FutureCallback<RES> cleanup = new FutureCallback<RES>() {
                @Override
                public void onSuccess(final RES result) {
                    futures.remove(submit);
                }

                @Override
                public void onFailure(final Throwable t) {
                    futures.remove(submit);
                }
            };
            FutureCallback<RES> callback = new FutureCallback<RES>() {
                @Override
                public void onSuccess(final RES successResult) {
                    event(
                            WorkflowSystemEventType.OperationSuccess,
                            String.format("operation succeeded: %s", successResult),
                            successResult
                    );
                    assert successResult != null;
                    OperationResult<DAT, RES, OP> result = result(successResult, operation);
                    synchronized (results) {
                        results.add(result);
                    }
                    stateChangeQueue.add(successResult);
                    synchronized (inProcess) {
                        inProcess.remove(operation);
                    }
                }

                @Override
                public void onFailure(final Throwable t) {
                    event(
                            WorkflowSystemEventType.OperationFailed,
                            String.format("operation failed: %s", t),
                            t
                    );
                    OperationResult<DAT, RES, OP> result = result(t, operation);
                    synchronized (results) {
                        results.add(result);
                    }
                    StateObj newFailureState = operation.getFailureState(t);
                    if (null != newFailureState && newFailureState.getState().size() > 0) {
                        OperationSuccess<DAT> objectOperationSuccess = dummyResult(newFailureState);
                        stateChangeQueue.add(objectOperationSuccess);
                    }
                    inProcess.remove(operation);

                }
            };
            Futures.addCallback(submit, callback, manager);
            Futures.addCallback(submit, cleanup, manager);
        }
    }

    private static <X, Y extends OperationSuccess<X>> Callable<Y> operationCallable(
            final Operation<X, Y> operation,
            final X input
    )
    {

        return new Callable<Y>() {
            @Override
            public Y call() throws Exception {
                return operation.apply(input);
            }
        };
    }

    static private <T> OperationSuccess<T> dummyResult(final StateObj state) {
        return new OperationSuccess<T>() {
            @Override
            public StateObj getNewState() {
                return state;
            }

            @Override
            public T getResult() {
                return null;
            }
        };
    }

    private void event(final WorkflowSystemEventType endOfChanges, final String message) {
        event(endOfChanges, message, null);
    }

    private void event(final WorkflowSystemEventType eventType, final String message, final Object data) {
        event(Event.with(eventType, message, data));
    }

    private void event(final WorkflowSystemEvent event) {
        logDebug(event.getMessage());

        if (null != listener) {
            listener.onEvent(event);
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


    private static class WResult<D, T extends OperationSuccess<D>, X extends Operation<D, T>> implements
            OperationResult<D, T, X>
    {
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

    private <D, T extends OperationSuccess<D>, X extends Operation<D, T>> OperationResult<D, T, X> result(
            final Throwable t, final X operation
    )
    {
        return new WResult<>(operation, t);
    }

    private <D, T extends OperationSuccess<D>, X extends Operation<D, T>> OperationResult<D, T, X> result(
            final T successResult, final X operation
    )
    {
        return new WResult<>(operation, successResult);
    }


    private Map<String, String> map(final String key, final String value) {
        HashMap<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    static private <DAT> void pollAllChanges(
            final LinkedBlockingQueue<OperationSuccess<DAT>> stateChangeQueue,
            final Map<String, String> changes,
            final SharedData<DAT> sharedData
    )
    {
        OperationSuccess<DAT> task = stateChangeQueue.poll();
        while (task != null && task.getNewState() != null) {
            changes.putAll(task.getNewState().getState());
            DAT result = task.getResult();
            if (null != sharedData) {
                sharedData.addData(result);
            }
            task = stateChangeQueue.poll();
        }
    }
}
