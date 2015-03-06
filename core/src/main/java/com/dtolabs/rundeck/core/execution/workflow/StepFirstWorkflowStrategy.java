/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* WFFirstWorkflowStrategy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Aug 26, 2010 2:16:49 PM
* $Id$
*/
package com.dtolabs.rundeck.core.execution.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;

/**
 * StepFirstWorkflowStrategy iterates over the workflow steps and dispatches each one to all nodes matching the filter.
 * This strategy is used either for an entire workflow and set of multiple nodes OR by the NodeFirstWorkflowStrategy as
 * the inner loop over a single node.
 * <br>
 * The WorkflowExecutionResult will contain as the resultSet a map of Node name to
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class StepFirstWorkflowStrategy extends BaseWorkflowStrategy {

    protected static final String DATA_CONTEXT_PREFIX = "data context: ";
    protected static final String OPTION_KEY = "option";
    protected static final String SECURE_OPTION_KEY = "secureOption";
    protected static final String SECURE_OPTION_VALUE = "****";
    
    public StepFirstWorkflowStrategy(final Framework framework) {
        super(framework);
    }

    public WorkflowExecutionResult executeWorkflowImpl(final StepExecutionContext executionContext,
                                                       final WorkflowExecutionItem item) {
        WorkflowStatusResult workflowResult= WorkflowResultFailed;
        Exception exception = null;
        final IWorkflow workflow = item.getWorkflow();
        final Map<Integer, StepExecutionResult> stepFailures = new HashMap<Integer, StepExecutionResult>();
        final List<StepExecutionResult> stepResults = new ArrayList<StepExecutionResult>();
        try {
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL,
                                                        "NodeSet: " + executionContext.getNodeSelector());
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Workflow: " + workflow);
            
            Map<String, Map<String, String>> printableContext = createPrintableDataContext(executionContext.getDataContext());
            executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, String.format("%s %s", DATA_CONTEXT_PREFIX, printableContext));

            final List<StepExecutionItem> iWorkflowCmdItems = workflow.getCommands();
            if (iWorkflowCmdItems.size() < 1) {
                executionContext.getExecutionListener().log(Constants.WARN_LEVEL, "Workflow has 0 items");
            }
            workflowResult = executeWorkflowItemsForNodeSet(executionContext, stepFailures, stepResults,
                                                             iWorkflowCmdItems, workflow.isKeepgoing());
        } catch (RuntimeException e) {
            exception = e;
            e.printStackTrace();
            executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Exception: " + e.getClass() + ": " + e
                    .getMessage());
        }
        final Exception orig = exception;
        final Map<String, Collection<StepExecutionResult>> nodeFailures = convertFailures(stepFailures);
        return new BaseWorkflowExecutionResult(
                stepResults,
                nodeFailures,
                stepFailures,
                orig,
                workflowResult
        );

    }
    
    /**
     * Creates a copy of the given data context with the secure option values obfuscated.
     * This does not modify the original data context.
     * 
     * "secureOption" map values will always be obfuscated. "option" entries that are also in "secureOption"
     * will have their values obfuscated. All other maps within the data context will be added
     * directly to the copy.
     * @param dataContext data
     * @return printable data
     */
    protected Map<String, Map<String, String>> createPrintableDataContext(Map<String, Map<String, String>> dataContext) {
        Map<String, Map<String, String>> printableContext = new HashMap<String, Map<String, String>>();
        if (dataContext != null) {
            printableContext.putAll(dataContext);
            Set<String> secureValues = new HashSet<String>();
            if (dataContext.containsKey(SECURE_OPTION_KEY)) {
                Map<String, String> secureOptions = new HashMap<String, String>();
                secureOptions.putAll(dataContext.get(SECURE_OPTION_KEY));
                secureValues.addAll(secureOptions.values());
                for (Map.Entry<String, String> entry : secureOptions.entrySet()) {
                    entry.setValue(SECURE_OPTION_VALUE);
                }
                printableContext.put(SECURE_OPTION_KEY, secureOptions);
            }

            if (dataContext.containsKey(OPTION_KEY)) {
                Map<String, String> options = new HashMap<String, String>();
                options.putAll(dataContext.get(OPTION_KEY));
                for (Map.Entry<String, String> entry : options.entrySet()) {
                    if (secureValues.contains(entry.getValue())) {
                        entry.setValue(SECURE_OPTION_VALUE);
                    }
                }
                printableContext.put(OPTION_KEY, options);
            }
        }
        return printableContext;
    }

    static boolean isInnerLoop(final WorkflowExecutionItem item) {
        return item.getWorkflow() instanceof stepFirstWrapper;
    }
    /**
     * Wrapper of IWorkflow that always returns STEP_FIRST for strategy
     */
    static class stepFirstWrapper implements IWorkflow {
        private IWorkflow workflow;

        stepFirstWrapper(IWorkflow workflow) {
            this.workflow = workflow;
        }

        public List<StepExecutionItem> getCommands() {
            return workflow.getCommands();
        }

        public int getThreadcount() {
            return workflow.getThreadcount();
        }

        public boolean isKeepgoing() {
            return workflow.isKeepgoing();
        }

        public String getStrategy() {
            return WorkflowExecutionItem.STEP_FIRST;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof stepFirstWrapper)) {
                return false;
            }

            stepFirstWrapper that = (stepFirstWrapper) o;

            if (workflow != null ? !workflow.equals(that.workflow) : that.workflow != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return workflow != null ? workflow.hashCode() : 0;
        }
    }
}
