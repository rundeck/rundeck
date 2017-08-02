/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.data.BaseDataContext;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.engine.OperationCompleted;
import com.dtolabs.rundeck.core.execution.workflow.engine.StepCallable;
import com.dtolabs.rundeck.core.execution.workflow.engine.StepOperation;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.rules.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;


/**
 * Primary executor for workflows
 */
public class EngineWorkflowExecutor extends BaseWorkflowExecutor {
    static final Logger logger = Logger.getLogger(EngineWorkflowExecutor.class);
    public static final String STEP_FLOW_CONTROL_KEY = "step.#.flowcontrol";
    public static final String STEP_ANY_FLOW_CONTROL_HALT_KEY = "step.any.flowcontrol.halt";
    public static final String STEP_FLOW_CONTROL_STATUS_KEY = "step.#.flowstatus";
    public static final String WORKFLOW_STATE_KEY = "workflow.state";
    public static final String WORKFLOW_KEEPGOING_KEY = "workflow.keepgoing";
    public static final String WORKFLOW_STATE_STARTED = "started";
    public static final String STEP_BEFORE_KEY = "before.step.#";
    public static final String STEP_AFTER_KEY = "after.step.#";
    public static final String STEP_STATE_KEY = "step.#.state";
    public static final String STEP_ANY_STATE_SKIPPED_KEY = "step.any.state.skipped";
    public static final String STEP_ANY_STATE_SUCCESS_KEY = "step.any.state.success";
    public static final String STEP_ANY_STATE_FAILED_KEY = "step.any.state.failed";
    public static final String STEP_COMPLETED_KEY = "step.#.completed";
    public static final String VALUE_TRUE = Boolean.TRUE.toString();
    private static final Rule FLOW_CONTROL_HALT_END_WORKFLOW = Rules.conditionsRule(
            Rules.equalsCondition(
                    STEP_ANY_FLOW_CONTROL_HALT_KEY,
                    VALUE_TRUE
            ),
            Workflows.getWorkflowEndState()
    );
    public static final String VALUE_FALSE = Boolean.FALSE.toString();
    private static final Rule STEP_FAILURE_KEEPGOING_FALSE_END_WORKFLOW = Rules.conditionsRule(
            Rules.conditionSet(
                    Rules.equalsCondition(
                            STEP_ANY_STATE_FAILED_KEY,
                            VALUE_TRUE
                    ),
                    Rules.equalsCondition(
                            WORKFLOW_KEEPGOING_KEY,
                            VALUE_FALSE
                    )
            ),
            Workflows.getWorkflowEndState()
    );
    private static final Set<Rule> INITIAL_RULES = Collections.unmodifiableSet(
            new HashSet<>(
                    Arrays.asList(
                            FLOW_CONTROL_HALT_END_WORKFLOW,
                            STEP_FAILURE_KEEPGOING_FALSE_END_WORKFLOW
                    )
            )
    );
    public static final String STEP_STATE_RESULT_SUCCESS = "success";
    public static final String STEP_STATE_RESULT_FAILURE = "failure";
    public static final String STEP_STATE_RESULT_SKIPPED = "skipped";
    public static final String STEP_CONTROL_KEY = "step.#.start";
    public static final String STEP_CONTROL_SKIP_KEY = "step.#.skip";
    public static final String STEP_CONTROL_START = "start";
    public static final String STEP_DATA_RESULT_KEY_PREFIX = "step.#.result.";
    private WorkflowSystemBuilder workflowSystemBuilder;

    public EngineWorkflowExecutor(final Framework framework) {
        super(framework);
        this.workflowSystemBuilder = Workflows.builder();
    }

    public static String stepKey(final String key, final Object stepNum) {
        return key.replace("#", stepNum.toString());
    }

    public static void updateStateWithStepResultData(
            final MutableStateObj state,
            final Object identity,
            final Map<String, Object> failureData
    )
    {
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        if (null != failureData) {
            for (String s : failureData.keySet()) {
                stringStringHashMap.put(
                        stepKey(STEP_DATA_RESULT_KEY_PREFIX + s, identity),
                        failureData.get(s).toString()
                );
            }
        }
        if (stringStringHashMap.size() > 0) {
            state.updateState(stringStringHashMap);
        }
    }

    public WorkflowSystemBuilder getWorkflowSystemBuilder() {
        return workflowSystemBuilder;
    }

    public void setWorkflowSystemBuilder(WorkflowSystemBuilder workflowSystemBuilder) {
        this.workflowSystemBuilder = workflowSystemBuilder;
    }


    /**
     * Base profile which provides initial states
     */
    public static abstract class BaseProfile implements WorkflowStrategyProfile {

        @Override
        public StateObj getInitialStateForStep(
                final int stepNum,
                final WorkflowExecutionItem item,
                final boolean isFirstStep
        )
        {

            MutableStateObj state = States.mutable(stepKey(STEP_BEFORE_KEY, stepNum), VALUE_TRUE);
            state.updateState(stepKey(STEP_AFTER_KEY, stepNum), VALUE_FALSE);
            return state;
        }

        @Override
        public Set<Condition> getSkipConditionsForStep(
                final WorkflowExecutionItem item,
                final int stepNum,
                final boolean isFirstStep
        )
        {
            return Collections.emptySet();
        }

    }


    @Override
    public WorkflowExecutionResult executeWorkflowImpl(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
    )
    {
        executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Start EngineWorkflowExecutor");
        final IWorkflow workflow = item.getWorkflow();

        final Map<Integer, StepExecutionResult> stepFailures = new HashMap<>();
        final List<StepExecutionResult> stepResults = new ArrayList<>();
        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
        String strategy = workflow.getStrategy();

        //define a new shared context inheriting from any injected data context
        final WFSharedContext sharedContext = WFSharedContext.withBase(executionContext.getSharedDataContext());
        WorkflowStrategy strategyForWorkflow;
        try {
            strategyForWorkflow = setupWorkflowStrategy(executionContext, item, workflow, strategy);
        } catch (ExecutionServiceException e) {
            executionContext.getExecutionListener().log(
                    Constants.ERR_LEVEL,
                    "Exception: " + e.getClass() + ": " + e.getMessage()
            );
            return new BaseWorkflowExecutionResult(
                    stepResults,
                    new HashMap<>(),
                    stepFailures,
                    e,
                    WorkflowResultFailed,
                    sharedContext
            );
        }

        MutableStateObj mutable = States.mutable(DataContextUtils.flattenDataContext(
                executionContext.getDataContext()));

        mutable.updateState(WORKFLOW_KEEPGOING_KEY, Boolean.toString(workflow.isKeepgoing()));

        MutableStateObj state = new StateLogger(mutable, executionContext.getExecutionListener());

        RuleEngine ruleEngine = setupRulesEngine(executionContext, workflow, strategyForWorkflow);

        WorkflowStrategyProfile profile = strategyForWorkflow.getProfile();
        if (profile == null) {
            profile = new SequentialStrategyProfile();
        }

        Set<StepOperation> operations
                = buildOperations(executionContext, item, workflow, wlistener, ruleEngine, state, profile);

        executionContext.getExecutionListener().log(
                Constants.DEBUG_LEVEL,
                "Create rule engine with rules: " + ruleEngine
        );
        executionContext.getExecutionListener().log(
                Constants.DEBUG_LEVEL,
                "Create workflow engine with state: " + state
        );

        WorkflowSystem workflowEngine = buildWorkflowSystem(
                executionContext,
                state,
                ruleEngine,
                strategyForWorkflow.getThreadCount()
        );



        WorkflowSystem.SharedData<WFSharedContext>
                dataContextSharedData = WorkflowSystem.SharedData.with(
                (data) -> {
                    sharedContext.merge(data);
//                    System.err.println("merge shared data: " + data + " result; " + sharedContext);
                },
                () -> {
//                    System.err.println("produce next shared data " + sharedContext);
                    return sharedContext;
                }
        );

        Set<WorkflowSystem.OperationResult<WFSharedContext, OperationCompleted, StepOperation>>
                operationResults = workflowEngine.processOperations(operations, dataContextSharedData);


        String statusString = null;
        ControlBehavior controlBehavior = null;


        boolean workflowSuccess = !workflowEngine.isInterrupted();
        for (WorkflowSystem.OperationResult<WFSharedContext, OperationCompleted, StepOperation> operationResult :
                operationResults) {
            OperationCompleted completed = operationResult.getSuccess();
            StepOperation operation = operationResult.getOperation();
            Throwable failure = operationResult.getFailure();

            if (completed != null) {
                StepResultCapture result = completed.getStepResultCapture();
                if (!result.getStepResult().isSuccess()) {
                    stepFailures.put(completed.getStepNum(), result.getStepResult());
                    workflowSuccess = false;
                }
                stepResults.add(result.getStepResult());
                if (result.getControlBehavior() != null && result.getControlBehavior() != ControlBehavior.Continue) {
                    controlBehavior = result.getControlBehavior();
                    statusString = result.getStatusString();
                }
//                System.out.println("Step result data: "+result.getResultData());//XXX
            } else {
                workflowSuccess = false;
                addUnknownStepFailure(executionContext, stepFailures, operation, failure);
            }
        }
        logSkippedOperations(executionContext, operations);

        WorkflowStatusResult workflowResult = workflowResult(
                workflowSuccess,
                statusString,
                null != controlBehavior ? controlBehavior : ControlBehavior.Continue,
                sharedContext
        );

        final Map<String, Collection<StepExecutionResult>> nodeFailures = convertFailures(stepFailures);
        return new BaseWorkflowExecutionResult(
                stepResults,
                nodeFailures,
                stepFailures,
                null,
                workflowResult,
                sharedContext
        );
    }

    public WorkflowSystem buildWorkflowSystem(
            final StepExecutionContext executionContext,
            final MutableStateObj state,
            final RuleEngine ruleEngine,
            final int wfThreadcount
    )
    {
        return getWorkflowSystemBuilder()
                .ruleEngine(ruleEngine)
                .executor(() -> wfThreadcount > 0
                                ? Executors.newFixedThreadPool(wfThreadcount)
                                : Executors.newCachedThreadPool()
                )
                .state(state)
                .listener(event -> executionContext.getExecutionListener()
                                                   .log(
                                                           Constants.DEBUG_LEVEL,
                                                           event.getEventType() + ": " + event.getMessage()
                                                   ))
                .build();
    }

    private void addUnknownStepFailure(
            final StepExecutionContext executionContext,
            final Map<Integer, StepExecutionResult> stepFailures,
            final StepOperation operation,
            final Throwable failure
    )
    {
        StepFailureReason reason = StepFailureReason.Unknown;

        String message = String.format(
                "Exception while executing step [%d]: \t[%s]",
                operation.getStepNum(),
                failure.toString()
        );
        failure.printStackTrace(System.out);//XXX
        if (failure instanceof CancellationException) {
            reason = StepFailureReason.Interrupted;

            message = String.format(
                    "Cancellation while running step [%d]",
                    operation.getStepNum()
            );
        }
        executionContext.getExecutionListener().log(Constants.ERR_LEVEL, message);
        stepFailures.put(operation.getStepNum(), StepExecutionResultImpl.wrapStepException(
                failure instanceof StepException ? (StepException) failure :
                new StepException(message, failure, reason)
        ));
    }

    private void logSkippedOperations(
            final StepExecutionContext executionContext,
            final Set<StepOperation> operations
    )
    {
        operations.stream()
                  .filter(op -> !op.isDidRun())
                  .forEach(op ->
                                   executionContext.getExecutionListener().log(
                                           Constants.WARN_LEVEL,
                                           String.format(
                                                   "Step [%d] did not run. " +
                                                   "start conditions: %s, skip " +
                                                   "conditions: %s",
                                                   op.getStepNum(),
                                                   op.getStartTriggerConditions(),
                                                   op.getSkipTriggerConditions()
                                           )
                                   )
                );
    }

    /**
     * Create and prepare a {@link RuleEngine} for processing operation rules
     *
     * @param executionContext    context
     * @param workflow            workflow
     * @param strategyForWorkflow strategy
     *
     * @return new RuleEngine
     */
    private RuleEngine setupRulesEngine(
            final StepExecutionContext executionContext,
            final IWorkflow workflow,
            final WorkflowStrategy strategyForWorkflow
    )
    {

        executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Building initial state and rules...");

        RuleEngine ruleEngine = Rules.createEngine(INITIAL_RULES);

        //add any additional strategy rules
        strategyForWorkflow.setup(ruleEngine, executionContext, workflow);
        return ruleEngine;
    }

    public WorkflowStrategy setupWorkflowStrategy(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item,
            final IWorkflow workflow,
            final String strategy
    ) throws ExecutionServiceException
    {
        final WorkflowStrategy strategyForWorkflow;
        Map<String, Object> pluginConfig = new HashMap<>();
        Map<String, Object> pluginConfig1 = workflow.getPluginConfig();
        if (pluginConfig1 != null) {
            Object o = pluginConfig1.get(ServiceNameConstants.WorkflowStrategy);
            if (o instanceof Map) {
                Object o1 = ((Map<String, Object>) o).get(strategy);
                if (o1 instanceof Map) {
                    pluginConfig = (Map<String, Object>) o1;
                }

            }
        }
        strategyForWorkflow = getFramework().getWorkflowStrategyService().getStrategyForWorkflow(
                item,
                pluginConfig,
                executionContext.getFrameworkProject()
        );
        return strategyForWorkflow;
    }

    private Set<StepOperation> buildOperations(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item,
            final IWorkflow workflow,
            final WorkflowExecutionListener wlistener,
            final RuleEngine ruleEngine,
            final MutableStateObj state,
            final WorkflowStrategyProfile profile
    )
    {
        final Set<StepOperation> operations = new HashSet<>();
        List<StepExecutionItem> commands = workflow.getCommands();
        for (int i = 0; i < commands.size(); i++) {
            final int stepNum = executionContext.getStepNumber() + i;
            StepExecutionItem cmd = workflow.getCommands().get(i);

            //add initial state of the step
            state.updateState(profile.getInitialStateForStep(stepNum, item, i == 0));

            // Conditions that indicate step should start
            Set<Condition> stepStartTriggerConditions = profile.getStartConditionsForStep(item, stepNum, i == 0);

            executionContext.getExecutionListener().log(
                    Constants.DEBUG_LEVEL,
                    String.format("start conditions for step [%d]: %s", stepNum, stepStartTriggerConditions)
            );
            // State that will trigger the step
            StateObj stepRunTriggerState = createTriggerControlStateForStep(stepNum);

            //add a rule to start the step when conditions are met
            ruleEngine.addRule(Rules.conditionsRule(stepStartTriggerConditions, stepRunTriggerState));

            Set<Condition> stepSkipTriggerConditions = profile.getSkipConditionsForStep(item, stepNum, i == 0);
            StateObj stepSkipTriggerState = null;
            if (null != stepSkipTriggerConditions && !stepSkipTriggerConditions.isEmpty()) {
                // State that will skip the step
                stepSkipTriggerState = createSkipTriggerStateForStep(stepNum);
                //add a rule to skip the step when its run condition is met, and its skip conditions are met
                ruleEngine.addRule(Rules.conditionsRule(
                        Condition.and(
                                Condition.and(stepStartTriggerConditions),
                                Condition.and(stepSkipTriggerConditions)
                        ),
                        stepSkipTriggerState
                ));
                executionContext.getExecutionListener().log(
                        Constants.DEBUG_LEVEL,
                        String.format("skip conditions for step [%d]: %s", stepNum, stepSkipTriggerConditions)
                );
            }

            operations.add(
                    new StepOperation(
                            stepNum,
                            cmd.getLabel(),
                            new StepCallable(
                                    this,
                                    executionContext,
                                    workflow.isKeepgoing(),
                                    wlistener,
                                    stepNum,
                                    cmd
                            ),
                            stepRunTriggerState,
                            stepSkipTriggerState,
                            stepStartTriggerConditions,
                            stepSkipTriggerConditions
                    )
            );
        }
        return operations;
    }



    private StateObj createTriggerControlStateForStep(final int stepNum) {
        return States.state(
                stepKey(STEP_CONTROL_KEY, stepNum),
                VALUE_TRUE
        );
    }

    private StateObj createSkipTriggerStateForStep(final int stepNum) {
        return States.state(
                stepKey(STEP_CONTROL_SKIP_KEY, stepNum),
                VALUE_TRUE
        );
    }

}