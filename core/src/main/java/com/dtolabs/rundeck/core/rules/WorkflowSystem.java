package com.dtolabs.rundeck.core.rules;

import com.google.common.base.Function;

import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Process a set of Operations, by use of a RuleEngine to determine when/if operations should run,
 * and by updating a shared state object with new state changes returned by each operation.
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
    <D, T extends OperationSuccess<D>, X extends Operation<D, T>> Set<OperationResult<D, T, X>> processOperations(
            final Set<X> operations,
            final SharedData<D> shared
    );

    /**
     * @return true if the previous call to {@link #processOperations(Set, SharedData)} stopped due to interruption
     */
    boolean isInterrupted();


    /**
     * Return type which contains either a success result, or a failure throwable, and includes the original
     * operation
     *
     * @param <T> success type
     * @param <X> operation type
     */
    public static interface OperationResult<D,T extends OperationSuccess<D>, X extends Operation<D,T>> {
        Throwable getFailure();

        T getSuccess();

        X getOperation();
    }

    /**
     * Manages shared data, consumes results of operations, and produces input for subsequent operations
     *
     * @param <T> data type
     */
    public static interface SharedData<T> {
        /**
         * Add a data item
         *
         * @param item
         */
        void addData(T item);

        /**
         * produce a data item for input to a subsequent operation
         *
         * @return
         */
        T produceNext();
    }

    /**
     * Indicates an operation succeeded, supplies a new set of state data to update the mutable state with
     */
    public static interface OperationSuccess<T> {
        StateObj getNewState();

        T getResult();
    }

    /**
     * function interface with throwable
     * @param <X>
     * @param <Y>
     */
    public static interface SimpleFunction<X,Y>{
        Y apply(X x) throws Exception;
    }
    /**
     * An operation which returns a success result object
     *
     * @param <T> result type
     */
    public static interface Operation<X, T extends OperationSuccess> extends SimpleFunction<X, T> {
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
