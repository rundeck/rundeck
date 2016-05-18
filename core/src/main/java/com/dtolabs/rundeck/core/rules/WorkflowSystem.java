package com.dtolabs.rundeck.core.rules;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Process a set of Operations, by use of a RuleEngine to determine when/if operations should run,
 * and by updating a shared state object with new state changes returned by each operation.
 * {@link WorkflowEngine} implementation: Operations can supply state changes after they succeed, or fail. State
 * changes
 * provided by
 * operations are added in the order they are received, and after a set of available state
 * changes are added, the rule engine is used to update the state based on its rules.  After
 * the state is updated, all pending operations are queried to see if they can run given the new
 * state, and any that can are queued to be executed.  Workflow processing stops when no operations
 * are currently running, no new state changes are available, and no pending operations can be run.
 */
public interface WorkflowSystem {

    /**
     * Process the operations and return results when all runnable operations are complete.
     *
     * @param operations operations
     * @param <T>        success result of an operation
     * @param <X>        operation class
     *
     * @return set of results for all processed operations
     */
    <T extends OperationSuccess, X extends Operation<T>> Set<OperationResult<T, X>> processOperations(
            final Set<X> operations
    );

    /**
     * @return true if the previous call to {@link #processOperations(Set)} stopped due to interruption
     */
    boolean isInterrupted();


    /**
     * Return type which contains either a success result, or a failure throwable, and includes the original
     * operation
     *
     * @param <T> success type
     * @param <X> operation type
     */
    public static interface OperationResult<T extends OperationSuccess, X extends Operation<T>> {
        Throwable getFailure();

        T getSuccess();

        X getOperation();
    }

    /**
     * Indicates an operation succeeded, supplies a new set of state data to update the mutable state with
     */
    public static interface OperationSuccess {
        StateObj getNewState();
    }

    /**
     * An operation which returns a success result object
     *
     * @param <T> result type
     */
    public static interface Operation<T extends OperationSuccess> extends Callable<T> {
        /**
         * @param state current state
         *
         * @return true if the operation should run given the state shown
         */
        boolean shouldRun(StateObj state);

        /**
         * @param state current state
         *
         * @return true if the operation should be skipped and never run
         */
        boolean shouldSkip(StateObj state);

        /**
         * @param state current state
         *
         * @return state change if operation is skipped
         */
        StateObj getSkipState(StateObj state);

        /**
         * @param t throwable
         *
         * @return new state changes if the operation failed
         */
        StateObj getFailureState(Throwable t);
    }

}
