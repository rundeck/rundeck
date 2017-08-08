package com.dtolabs.rundeck.core.rules;

import com.google.common.util.concurrent.*;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

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
    private final MutableStateObj state;
    private final RuleEngine engine;
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

    MutableStateObj getState() {
        return state;
    }

    RuleEngine getRuleEngine() {
        return engine;
    }

    static class Sleeper {
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
            RES extends OperationCompleted<DAT>,
            OP extends Operation<DAT, RES>
            >
    Set<OperationResult<DAT, RES, OP>> processOperations(final Set<OP> operations, final SharedData<DAT> sharedData) {

        WorkflowEngineOperationsProcessor<DAT, RES, OP> processor = new WorkflowEngineOperationsProcessor<>(
                this,
                operations,
                sharedData,
                executorService,
                manager
        );

        processor.beginProcessing();

        Set<OperationResult<DAT, RES, OP>> results = processor.getResults();
        interrupted = processor.isInterrupted();

        event(WorkflowSystemEventType.WillShutdown, "Workflow engine shutting down");

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }

        manager.shutdown();
        try {
            if (!manager.awaitTermination(5, TimeUnit.MINUTES)) {
                manager.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        if (!processor.getPending().isEmpty()) {
            event(
                    WorkflowSystemEventType.IncompleteOperations,
                    String.format("Some operations were not run: %d", processor.getPending().size()),
                    processor.getPending()
            );
        }
        event(WorkflowSystemEventType.Complete, String.format("Workflow complete: %s", results));
        return results;
    }

    void eventLoopProgress(
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


    static <T> OperationCompleted<T> dummyResult(final StateObj state) {
        return new OperationCompleted<T>() {
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

    void event(final WorkflowSystemEventType endOfChanges, final String message) {
        event(endOfChanges, message, null);
    }

    void event(final WorkflowSystemEventType eventType, final String message, final Object data) {
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


    static class WResult<D, T extends OperationCompleted<D>, X extends Operation<D, T>> implements
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



    private Map<String, String> map(final String key, final String value) {
        HashMap<String, String> map = new HashMap<>();
        map.put(key, value);
        return map;
    }


}
