/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
import com.dtolabs.rundeck.core.NodesetEmptyException;
import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.NodesSelector;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
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
import com.google.common.base.Throwables;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.function.Supplier;


/**
 * Primary executor for workflows
 */
public class EngineWorkflowExecutor extends BaseWorkflowExecutor {
    static final        Logger logger                         = LoggerFactory.getLogger(EngineWorkflowExecutor.class);
    public static final String STEP_FLOW_CONTROL_KEY          = "step.#.flowcontrol";
    public static final String STEP_ANY_FLOW_CONTROL_HALT_KEY = "step.any.flowcontrol.halt";
    public static final String STEP_FLOW_CONTROL_STATUS_KEY   = "step.#.flowstatus";
    public static final String WORKFLOW_STATE_KEY             = "workflow.state";
    public static final String WORKFLOW_KEEPGOING_KEY         = "workflow.keepgoing";
    public static final String WORKFLOW_STATE_STARTED         = "started";
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
    @Getter @Setter private Supplier<WorkflowSystemBuilder> workflowSystemBuilderSupplier;

    public EngineWorkflowExecutor(final IFramework framework) {
        super(framework);
        this.setWorkflowSystemBuilderSupplier(Workflows::builder);
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
            return Collections.singleton(Rules.equalsCondition(stepKey(STEP_COMPLETED_KEY, stepNum), VALUE_TRUE));
        }

    }

    static interface LogOut {
        void log(String message);
    }

    /**
     * Can augment behavior of this workflow executor
     */
    public interface Augmentor {
        MutableStateObj getInitialState(
                final WorkflowExecutionItem item,
                final StepExecutionContext executionContext
        );

        WFSharedContext getSharedContext(final StepExecutionContext executionContext);
    }

    @Override
    public WorkflowExecutionResult executeWorkflowImpl(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
    )
    {
        Augmentor component = executionContext.useSingleComponentOfType(Augmentor.class)
                                              .orElseGet(DefaultAugmentor::new);

        MutableStateObj mutable = component.getInitialState(item, executionContext);
        final WFSharedContext sharedContext = component.getSharedContext(executionContext);

        final String workflowId = mutable.getState().get(Workflows.WORKFLOW_STATE_ID_KEY);

        ExecutionListener executionListener = executionContext.getExecutionListener();
        LogOut logDebug = createDebugLog(workflowId, executionListener);
        LogOut logWarn = createWarnLog(workflowId, executionListener);
        LogOut logErr = createErrLog(workflowId, executionListener);
        logDebug.log("Start EngineWorkflowExecutor");

        MutableStateObj state = new StateLogger(mutable, logDebug::log);

        final IWorkflow workflow = item.getWorkflow();
        final Map<Integer, StepExecutionResult> stepFailures = new HashMap<>();
        final List<StepExecutionResult> stepResults = new ArrayList<>();


        WorkflowStrategy strategyForWorkflow;
        WorkflowStatusResult workflowResult = null;
        Exception exception = null;
        try {
            strategyForWorkflow = setupWorkflowStrategy(executionContext, item, workflow, getFramework());

            final NodesSelector nodeSelector = executionContext.getNodeSelector();
            validateNodeSet(executionContext, nodeSelector);
            RuleEngine ruleEngine = setupRulesEngine(executionContext, workflow, strategyForWorkflow);

            WorkflowStrategyProfile profile = strategyForWorkflow.getProfile();
            if (profile == null) {
                profile = new SequentialStrategyProfile();
            }

            final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
            Set<StepOperation> operations = buildOperations(
                    this,
                    executionContext,
                    item,
                    workflow,
                    wlistener,
                    ruleEngine,
                    state,
                    profile,
                    logDebug
            );

            logDebug.log("Create rule engine with rules: " + ruleEngine);
            logDebug.log("Create workflow engine with state: " + state);

            List<WorkflowSystemEventListener> list = new ArrayList<>();

            list.add(event -> {
                logDebug.log(String.format("%s: %s", event.getEventType(), event.getMessage()));
            });

            executionContext.useAllComponentsOfType(WorkflowSystemEventListener.class, list::add);

            WorkflowSystem<Map<String, String>> workflowEngine = buildWorkflowSystem(
                    state,
                    ruleEngine,
                    strategyForWorkflow.getThreadCount(),
                    getWorkflowSystemBuilderSupplier(),
                    list
            );

            WorkflowSystem.SharedData<WFSharedContext, Map<String, String>>
                    dataContextSharedData =
                    WorkflowSystem.SharedData.with(
                            sharedContext::merge,
                            () -> sharedContext,
                            () -> DataContextUtils.flattenDataContext(sharedContext.consolidate().getData(ContextView.global()))
                    );

            Set<WorkflowSystem.OperationResult<WFSharedContext, OperationCompleted, StepOperation>>
                    operationResults =
                    workflowEngine.processOperations(operations, dataContextSharedData);


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
                } else {
                    workflowSuccess = false;
                    addUnknownStepFailure(executionContext, stepFailures, operation, failure, logDebug, logErr);
                }
            }
            logSkippedOperations(executionContext, operations, logWarn);

            workflowResult = workflowResult(
                    workflowSuccess,
                    statusString,
                    null != controlBehavior ? controlBehavior : ControlBehavior.Continue,
                    sharedContext
            );
        } catch (ExecutionServiceException e) {
            logErr.log("Exception: " + e.getClass() + ": " + e.getMessage());
            return new BaseWorkflowExecutionResult(
                    stepResults,
                    new HashMap<>(),
                    stepFailures,
                    e,
                    WorkflowResultFailed,
                    sharedContext
            );
        } catch (NodesetEmptyException e) {
            Boolean successOnEmptyNodeFilter = Boolean.valueOf(executionContext.getDataContext()
                    .get("job")
                    .get("successOnEmptyNodeFilter"));
            if (!successOnEmptyNodeFilter) {
                exception = e;
                e.printStackTrace();
                executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Exception: " + e.getClass() + ": " + e
                        .getMessage());
                workflowResult = WorkflowResultFailed;
            } else {
                logDebug.log("No matched nodes");
                workflowResult = workflowResult(true, null, ControlBehavior.Continue, sharedContext);
            }
        }
        final Exception fexception = exception;

        final Map<String, Collection<StepExecutionResult>> nodeFailures = convertFailures(stepFailures);
        return new BaseWorkflowExecutionResult(
                stepResults,
                nodeFailures,
                stepFailures,
                fexception,
                workflowResult,
                sharedContext
        );
    }

    private static LogOut createDebugLog(
            final String workflowId,
            final ExecutionListener executionListener
    )
    {
        return (String message) -> {
            String logMessage = String.format("[wf:%s] %s", workflowId, message);
            logger.debug(logMessage);
            executionListener.log(Constants.DEBUG_LEVEL, logMessage);
        };
    }

    private static LogOut createWarnLog(
            final String workflowId,
            final ExecutionListener executionListener
    )
    {
        return (String message) -> {
            String logMessage = String.format("[wf:%s] %s", workflowId, message);
            logger.warn(logMessage);
            executionListener.log(Constants.WARN_LEVEL, logMessage);
        };
    }

    private static LogOut createErrLog(
            final String workflowId,
            final ExecutionListener executionListener
    )
    {
        return (String message) -> {
            String logMessage = String.format("[wf:%s] %s", workflowId, message);
            logger.error(logMessage);
            executionListener.log(Constants.ERR_LEVEL, logMessage);
        };
    }

    private static WorkflowSystem<Map<String, String>> buildWorkflowSystem(
            final MutableStateObj state,
            final RuleEngine ruleEngine,
            final int wfThreadcount,
            final Supplier<WorkflowSystemBuilder> workflowSystemBuilder,
            final List<WorkflowSystemEventListener> workflowSystemEventListeners
    )
    {
        return workflowSystemBuilder.get()
                                    .ruleEngine(ruleEngine)
                                    .executor(() -> wfThreadcount > 0
                                                    ? Executors.newFixedThreadPool(wfThreadcount)
                                                    : Executors.newCachedThreadPool()
                                    )
                                    .state(state)
                                    .listeners(workflowSystemEventListeners)
                                    .build();
    }

    private static void addUnknownStepFailure(
            final StepExecutionContext executionContext,
            final Map<Integer, StepExecutionResult> stepFailures,
            final StepOperation operation,
            final Throwable failure,
            final LogOut logDebug,
            final LogOut logErr
    )
    {
        StepFailureReason reason = StepFailureReason.Unknown;

        String message = String.format(
                "Exception while executing step [%d]: \t[%s]",
                operation.getStepNum(),
                failure.toString()
        );

        if (failure instanceof CancellationException || failure instanceof java.lang.InterruptedException) {
            reason = StepFailureReason.Interrupted;

            message = String.format(
                "Cancellation while running step [%d]",
                operation.getStepNum()
            );
        } else {
            logDebug.log(message + ": " + Throwables.getStackTraceAsString(failure));
        }
        logErr.log(message);
        stepFailures.put(operation.getStepNum(), StepExecutionResultImpl.wrapStepException(
                failure instanceof StepException ? (StepException) failure :
                new StepException(message, failure, reason)
        ));
    }

    private static void logSkippedOperations(
            final StepExecutionContext executionContext,
            final Set<StepOperation> operations,
            LogOut logOut
    )
    {
        operations.stream()
                  .filter(op -> !op.isDidRun())
                  .forEach(op ->
                                   logOut.log(
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
    private static RuleEngine setupRulesEngine(
            final StepExecutionContext executionContext,
            final IWorkflow workflow,
            final WorkflowStrategy strategyForWorkflow
    )
    {


        RuleEngine ruleEngine = Rules.createEngine(INITIAL_RULES);

        //add any additional strategy rules
        strategyForWorkflow.setup(ruleEngine, executionContext, workflow);
        return ruleEngine;
    }

    public static WorkflowStrategy setupWorkflowStrategy(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item,
            final IWorkflow workflow,
            final IFramework framework
    ) throws ExecutionServiceException
    {
        final WorkflowStrategy strategyForWorkflow;
        Map<String, Object> pluginConfig = new HashMap<>();
        Map<String, Object> pluginConfig1 = workflow.getPluginConfig();
        if (pluginConfig1 != null) {
            Object o = pluginConfig1.get(ServiceNameConstants.WorkflowStrategy);
            if (o instanceof Map) {
                Object o1 = ((Map<String, Object>) o).get(workflow.getStrategy());
                if (o1 instanceof Map) {
                    pluginConfig = (Map<String, Object>) o1;
                }

            }
        }
        strategyForWorkflow = framework.getWorkflowStrategyService().getStrategyForWorkflow(
                item,
                pluginConfig,
                executionContext.getFrameworkProject()
        );
        return strategyForWorkflow;
    }

    protected static Set<StepOperation> buildOperations(
            final EngineWorkflowExecutor engineWorkflowExecutor,
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item,
            final IWorkflow workflow,
            final WorkflowExecutionListener wlistener,
            final RuleEngine ruleEngine,
            final MutableStateObj state,
            final WorkflowStrategyProfile profile,
            LogOut log
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

            log.log(
                    String.format("start conditions for step [%d]: %s", stepNum, stepStartTriggerConditions)
            );
            // State that will trigger the step
            StateObj stepRunTriggerState = createTriggerControlStateForStep(stepNum);

            //add a rule to start the step when conditions are met
            ruleEngine.addRule(Rules.conditionsRule(stepStartTriggerConditions, stepRunTriggerState));

            Set<Condition> stepSkipTriggerConditions;
            if(workflow.getCommands().get(i).isEnabled()){
                stepSkipTriggerConditions = profile.getSkipConditionsForStep(item, stepNum, i == 0);
            }else{
                stepSkipTriggerConditions = Collections.singleton(Rules.equalsCondition(stepKey(STEP_BEFORE_KEY, stepNum), VALUE_TRUE));
            }



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
                log.log(
                        String.format("skip conditions for step [%d]: %s", stepNum, stepSkipTriggerConditions)
                );
            }

            operations.add(
                    new StepOperation(
                            stepNum,
                            cmd.getLabel(),
                            new StepCallable(
                                    engineWorkflowExecutor,
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


    private static StateObj createTriggerControlStateForStep(final int stepNum) {
        return States.state(
                stepKey(STEP_CONTROL_KEY, stepNum),
                VALUE_TRUE
        );
    }

    private static StateObj createSkipTriggerStateForStep(final int stepNum) {
        return States.state(
                stepKey(STEP_CONTROL_SKIP_KEY, stepNum),
                VALUE_TRUE
        );
    }

    public static class DefaultAugmentor
            implements Augmentor
    {

        @Override
        public MutableStateObj getInitialState(
                final WorkflowExecutionItem item,
                final StepExecutionContext executionContext
        )
        {
            MutableStateObj mutable = States.mutable();
            mutable.updateState(Workflows.getNewWorkflowState());
            mutable.updateState(WORKFLOW_KEEPGOING_KEY, Boolean.toString(item.getWorkflow().isKeepgoing()));
            return mutable;
        }

        @Override
        public WFSharedContext getSharedContext(final StepExecutionContext executionContext) {
            //define a new shared context inheriting from any injected data context
            return WFSharedContext.withBase(executionContext.getSharedDataContext());
        }
    }
}
