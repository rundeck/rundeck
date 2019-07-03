package com.dtolabs.rundeck.core.rules;

import com.google.common.util.concurrent.*;
import lombok.Getter;
import lombok.Setter;
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
public class WorkflowEngine
        implements StateWorkflowSystem, WorkflowSystemEventHandler
{
    static Logger logger = Logger.getLogger(WorkflowEngine.class.getName());
    @Getter
    private final MutableStateObj state;
    @Getter
    private final RuleEngine ruleEngine;
    private final ListeningExecutorService executorService;
    private final ListeningExecutorService manager;

    @Getter
    @Setter
    private List<WorkflowSystemEventListener> listeners;
    @Getter
    @Setter
    private volatile boolean interrupted;

    /**
     * Create engine
     *
     * @param ruleEngine   rule engine to process state changes via rules
     * @param state    initial state
     * @param executor executor to process operations, which should be multithreaded to process operations concurrently
     */
    public WorkflowEngine(
            final RuleEngine ruleEngine,
            final MutableStateObj state,
            final ExecutorService executor

    )
    {
        this.ruleEngine = ruleEngine;
        this.state = state;
        executorService = MoreExecutors.listeningDecorator(executor);
        manager = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
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
                this,
                operations,
                sharedData,
                executorService,
                manager
        );

        processor.beginProcessing();

        Set<OperationResult<DAT, RES, OP>> results = processor.getResults();
        interrupted = processor.isInterrupted();

        event(
                WorkflowSystemEventType.WillShutdown,
                String.format("Workflow engine shutting down (interrupted? %s)", interrupted),
                interrupted
        );

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

    @Override
    public boolean processStateChanges(final Map<String, String> changes) {
        event(WorkflowSystemEventType.WillProcessStateChange,
              String.format("saw state changes: %s", changes), changes
        );

        getState().updateState(changes);

        boolean update = Rules.update(getRuleEngine(), getState());
        event(
                WorkflowSystemEventType.DidProcessStateChange,
                String.format(
                        "applied state changes and rules (changed? %s): %s",
                        update,
                        getState()
                ),
                getState()
        );

        if (isWorkflowEndState()) {
            event(
                    WorkflowSystemEventType.WorkflowEndState,
                    "Workflow end state reached.",
                    getState()
            );
            return false;
        }
        return true;
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

    @Override
    public void event(final WorkflowSystemEventType eventType, final String message) {
        event(eventType, message, null);
    }

    @Override
    public void event(final WorkflowSystemEventType eventType, final String message, final Object data) {
        event(Event.with(eventType, message, data));
    }

    @Override
    public void event(final WorkflowSystemEvent event) {
        if (null != listeners && !listeners.isEmpty()) {
            listeners.forEach(a -> a.onEvent(event));
        }
    }

    @Override
    public boolean isWorkflowEndState() {
        return getState().hasState(Workflows.getWorkflowEndState());
    }

    @Override
    public boolean isInterrupted() {
        return interrupted;
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

        @Override
        public String toString() {
            return operation +
                   ": " +
                   (null != success ? success : null != throwable ? throwable.getClass().getSimpleName() : "?");
        }
    }
}
