package com.dtolabs.rundeck.core.rules;

import com.google.common.util.concurrent.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    static        Logger                   logger = LoggerFactory.getLogger(WorkflowEngine.class.getName());
    @Getter
    private final MutableStateObj          state;
    @Getter
    private final RuleEngine               ruleEngine;
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

    @Override
    public <DAT,
            RES extends OperationCompleted<DAT>,
            OP extends Operation<DAT, RES>
            >
    Set<OperationResult<DAT, RES, OP>> processOperations(
            final Set<OP> operations,
            final SharedData<DAT, Map<String, String>> sharedData
    )
    {

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
        event(WorkflowSystemEventType.Complete, String.format("Workflow complete: %s", results),
              StateWorkflowSystem.stateEvent(getState(), sharedData)
        );
        return results;
    }

    @Override
    public <D> boolean processStateChange(
            final String identity,
            final StateObj newState,
            final D newData,
            final SharedData<D, Map<String, String>> sharedData
    )
    {
        event(
                WorkflowSystemEventType.WillProcessStateChange,
                String.format("state changes: %s %s", identity, newState),
                StateWorkflowSystem.stateChangeEvent(
                        identity,
                        getState(),
                        StateWorkflowSystem.stateChange(identity, newState, newData, sharedData)
                )
        );

        if (null != newData) {
            sharedData.addData(newData);
        }
        Map<String, String> additionalState = sharedData != null?sharedData.produceState():null;

        boolean update = getState().updateState(newState);
        D nextShared = sharedData != null ? sharedData.produceNext() : null;

        StateObj additional = additionalState != null ? States.state(additionalState) : null;
        update |= Rules.update(getRuleEngine(), getState(), additional);
        event(
                WorkflowSystemEventType.DidProcessStateChange,
                String.format(
                        "applied state changes and rules (changed? %s): %s - %s",
                        update,
                        identity,
                        getState()
                ),
                StateWorkflowSystem.stateChangeEvent(
                        identity,
                        getState(),
                        StateWorkflowSystem.stateChange(identity, additional, nextShared, sharedData)
                )
        );

        return update;
    }

    @ToString
    static class Event implements WorkflowSystemEvent {
        @Getter @Setter private WorkflowSystemEventType eventType;
        @Getter @Setter private String message;
        @Getter @Setter private Object data;

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
    }


    static <T> OperationCompleted<T> dummyResult(final StateObj state, final String identity, final boolean success) {
        return new WFOperationCompleted<>(identity, state, success);
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
            listeners.forEach(a -> {
                try {
                    a.onEvent(event);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public boolean isWorkflowEndState() {
        return getState().hasState(Workflows.getWorkflowEndState());
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

    @RequiredArgsConstructor
    private static class WFOperationCompleted<T>
            implements OperationCompleted<T>
    {
        @Getter private final String identity;
        @Getter private final StateObj newState;
        @Getter private final boolean success;

        @Override
        public T getResult() {
            return null;
        }
    }
}
