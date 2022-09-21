/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Uses a mutable state and rule engine
 */
public interface StateWorkflowSystem
        extends WorkflowSystem<Map<String, String>>
{
    /**
     * state object
     */
    MutableStateObj getState();

    /**
     * Rule engine
     */
    RuleEngine getRuleEngine();

    /**
     * state change for given identity
     */
    interface StateChange<D> {
        /**
         * identity for change
         */
        String getIdentity();

        /**
         * @return new state data
         */
        StateObj getState();

        /**
         * @return new state data
         */
        D getNewData();

        /**
         * @return shared data
         */
        SharedData<D, Map<String, String>> getSharedData();
    }

    /**
     * Create State Change
     *
     * @param identity
     * @param state
     * @param sharedData
     * @param <D>
     */
    static <D> StateChange<D> stateChange(
            String identity,
            StateObj state,
            final D newData,
            SharedData<D, Map<String, String>> sharedData
    )
    {
        return new StateWorkflowSystem.StateChange<D>() {
            @Override
            public String getIdentity() {
                return identity;
            }

            @Override
            public StateObj getState() {
                return state;
            }

            @Override
            public D getNewData() {
                return newData;
            }

            @Override
            public SharedData<D, Map<String, String>> getSharedData() {
                return sharedData;
            }
        };
    }

    /**
     * state change for workflow
     */
    interface StateEvent<D> {

        /**
         * Current state
         */
        MutableStateObj getState();

        SharedData<D, Map<String, String>> getSharedData();
    }


    /**
     * Create StateEvent
     *
     * @param state
     * @param sharedData
     * @param <D>
     */
    static <D> StateEvent<D> stateEvent(

            MutableStateObj state,
            WorkflowSystem.SharedData<D, Map<String, String>> sharedData
    )
    {
        return new StateWorkflowSystem.StateEvent<D>() {

            @Override
            public MutableStateObj getState() {
                return state;
            }

            @Override
            public SharedData<D, Map<String, String>> getSharedData() {
                return sharedData;
            }
        };
    }

    /**
     * state change event for given identity
     */
    interface StateChangeEvent<D> {
        /**
         *
         * @return identity of step
         */
        String getIdentity();
        /**
         * Current state
         */
        public MutableStateObj getState();

        /**
         * State change
         */
        StateChange<D> getStateChange();
    }

    /**
     * Create StateChangeEvent
     *
     * @param <D>
     * @param identity
     * @param state
     * @param stateChange
     */
    static <D> StateChangeEvent<D> stateChangeEvent(
            final String identity,
            MutableStateObj state,
            StateChange<D> stateChange
    )
    {
        return new StateWorkflowSystem.StateChangeEvent<D>() {
            @Override
            public String getIdentity() {
                return identity;
            }

            @Override
            public MutableStateObj getState() {
                return state;
            }

            @Override
            public StateChange<D> getStateChange() {
                return stateChange;
            }
        };
    }

    /**
     * operation event with identity
     */
    interface OperationEvent<D>
            extends StateEvent<D>
    {
        /**
         * Get operation identity
         */
        String getIdentity();
    }


    /**
     * Create OperationEvent
     *
     * @param identity
     * @param state
     * @param sharedData
     * @param <D>
     */
    static <D> OperationEvent<D> operationEvent(
            String identity,
            MutableStateObj state,
            WorkflowSystem.SharedData<D, Map<String, String>> sharedData
    )
    {
        return new StateWorkflowSystem.OperationEvent<D>() {
            @Override
            public String getIdentity() {
                return identity;
            }

            @Override
            public MutableStateObj getState() {
                return state;
            }

            @Override
            public SharedData<D, Map<String, String>> getSharedData() {
                return sharedData;
            }
        };
    }


    /**
     * operation completed event
     */
    interface OperationCompleteEvent<D, RES extends OperationCompleted<D>, OP extends Operation<D, RES>>
            extends OperationEvent<D>
    {
        /**
         * @return result of operation
         */
        WorkflowSystem.OperationResult<D, RES, OP> getResult();
    }


    /**
     * Create OperationCompleteEvent
     * @param identity
     * @param state
     * @param sharedData
     * @param result
     * @param <D>
     * @param <RES>
     * @param <OP>
     * @return
     */
    static <D, RES extends OperationCompleted<D>, OP extends Operation<D, RES>> OperationCompleteEvent<D, RES, OP> operationCompleteEvent(
            String identity,
            MutableStateObj state,
            WorkflowSystem.SharedData<D, Map<String, String>> sharedData,
            final OperationResult<D, RES, OP> result
    )
    {
        return new StateWorkflowSystem.OperationCompleteEvent<D, RES, OP>() {
            @Override
            public String getIdentity() {
                return identity;
            }

            @Override
            public MutableStateObj getState() {
                return state;
            }

            @Override
            public SharedData<D, Map<String, String>> getSharedData() {
                return sharedData;
            }

            @Override
            public OperationResult<D, RES, OP> getResult() {
                return result;
            }
        };
    }

    /**
     * Handle the state changes for the rule engine
     *
     * @param identity step identity
     * @param state state change map
     * @param newData new data provided by operation
     * @param sharedData shared data
     * @return true if internal state was changed
     */
    <D> boolean processStateChange(
            final String identity,
            final StateObj state,
            final D newData,
            final SharedData<D, Map<String, String>> sharedData
    );

    /**
     * @return true if the state indicates the workflow should end
     */
    boolean isWorkflowEndState();

    /**
     * listener
     */
    List<WorkflowSystemEventListener> getListeners();

    /**
     * set listener
     *
     * @param listeners
     */
    void setListeners(List<WorkflowSystemEventListener> listeners);
}
