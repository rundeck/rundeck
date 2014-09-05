/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepStateChange

/**
 * Catch illegal state exception and runtime exception for state changes and allow logging without interrupting execution
 */
class ExceptionHandlingMutableWorkflowState extends DelegateMutableWorkflowState {

    ExceptionHandlingMutableWorkflowState(MutableWorkflowState delegate) {
        super(delegate)
    }
    Closure illegalStateHandler
    Closure runtimeExceptionHandler
    @Override
    void updateStateForStep(StepIdentifier identifier, int index, StepStateChange stepStateChange, Date timestamp) {
        try{
            delegate.updateStateForStep(identifier,index,stepStateChange,timestamp)
        }catch (IllegalStateException e) {
            if (illegalStateHandler || runtimeExceptionHandler) {
                (illegalStateHandler ?: runtimeExceptionHandler)("updateStateForStep("+[identifier:identifier, index:index, stepStateChange: stepStateChange,
                        timestamp: timestamp]+")", e)
            }
        }catch(RuntimeException e){
            if (runtimeExceptionHandler) {
                runtimeExceptionHandler("updateStateForStep(" + [identifier: identifier, index: index, stepStateChange: stepStateChange,
                        timestamp: timestamp] + ")", e)
            }
        }
    }

    @Override
    void updateWorkflowState(ExecutionState executionState, Date timestamp, List<String> nodeNames) {
        try {
            delegate.updateWorkflowState(executionState,timestamp,nodeNames)
        } catch (IllegalStateException e) {
            if (illegalStateHandler || runtimeExceptionHandler) {
                (illegalStateHandler ?: runtimeExceptionHandler)("updateWorkflowState("+[executionState: executionState, timestamp: timestamp, nodeNames: nodeNames]+")", e)
            }
        } catch (RuntimeException e) {
            if (runtimeExceptionHandler) {
                runtimeExceptionHandler("updateWorkflowState(" + [executionState: executionState, timestamp: timestamp, nodeNames: nodeNames] + ")", e)
            }
        }
    }

    @Override
    void updateSubWorkflowState(StepIdentifier identifier, int index, boolean quellFinalState, ExecutionState executionState, Date timestamp,
                                List<String> nodeNames, MutableWorkflowState parent) {
        try {
            delegate.updateSubWorkflowState(identifier,index,quellFinalState,executionState,timestamp,nodeNames,parent)
        } catch (IllegalStateException e) {
            if (illegalStateHandler || runtimeExceptionHandler) {
                (illegalStateHandler ?: runtimeExceptionHandler)("updateSubWorkflowState("+[
                        identifier:identifier, index:index, executionState:executionState, timestamp:timestamp,
                        quellFinalState:quellFinalState,
                        nodeNames:nodeNames, parent: parent
                ]+")",e)
            }
        } catch (RuntimeException e) {
            if (runtimeExceptionHandler) {
                runtimeExceptionHandler("updateSubWorkflowState(" + [
                        identifier: identifier, index: index, executionState: executionState, timestamp: timestamp,
                        quellFinalState: quellFinalState,
                        nodeNames: nodeNames, parent: parent
                ] + ")", e)
            }
        }
    }

}
