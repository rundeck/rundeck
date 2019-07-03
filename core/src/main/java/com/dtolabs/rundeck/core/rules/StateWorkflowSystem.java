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

/**
 * Uses a mutable state and rule engine
 */
public interface StateWorkflowSystem
        extends WorkflowSystem
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
     * Handle the state changes for the rule engine
     *
     * @param changes
     * @return true to continue processing, false if end state is reached
     */
    boolean processStateChanges(Map<String, String> changes);

    /**
     * @return true if the state indicates the workflow should end
     */
    boolean isWorkflowEndState();

    /**
     * listener
     */
    List<WorkflowSystemEventListener> getListeners();

    /**
     * set listeners
     *
     * @param listeners
     */
    void setListeners(List<WorkflowSystemEventListener> listeners);
}
