package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.ExecutionListener;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.steps.*;
import com.dtolabs.rundeck.core.rules.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutorService;
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
    public static final String VALUE_FALSE = Boolean.FALSE.toString();
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
            final Map<String, Object> resultData,
            final Map<String, Object> failureData
    )
    {
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        if (null != resultData) {
            for (String s : resultData.keySet()) {
                stringStringHashMap.put(
                        stepKey(STEP_DATA_RESULT_KEY_PREFIX + s, identity),
                        resultData.get(s).toString()
                );
            }
        }
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
            return null;
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

        List<StepExecutionItem> commands = workflow.getCommands();
        int stepCount = commands.size();

        final Map<Integer, StepExecutionResult> stepExecutionResults = new HashMap<>();
        final Map<Integer, StepExecutionResult> stepFailures = new HashMap<>();
        final List<StepExecutionResult> stepResults = new ArrayList<>();
        final WorkflowExecutionListener wlistener = getWorkflowListener(executionContext);
        String strategy = workflow.getStrategy();

        WorkflowStrategy strategyForWorkflow;
        try {
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
            strategyForWorkflow = framework.getWorkflowStrategyService().getStrategyForWorkflow(
                    item,
                    pluginConfig,
                    executionContext.getFrameworkProject()
            );
        } catch (ExecutionServiceException e) {
            executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Exception: " + e.getClass() + ": " + e
                    .getMessage());
            return new BaseWorkflowExecutionResult(
                    stepResults,
                    new HashMap<String, Collection<StepExecutionResult>>(),
                    stepFailures,
                    e,
                    WorkflowResultFailed
            );
        }

        RuleEngine ruleEngine = Rules.createEngine();

        MutableStateObj mutable = States.mutable(DataContextUtils.flattenDataContext(
                executionContext.getDataContext()));
        mutable.updateState(WORKFLOW_KEEPGOING_KEY, Boolean.toString(workflow.isKeepgoing()));
        MutableStateObj state = new StateLogger(
                mutable,
                executionContext.getExecutionListener()
        );

        int wfThreadcount = strategyForWorkflow.getThreadCount();
        //strategy.equals("parallel") ? 0 : workflow
        // .getThreadcount();

        ExecutorService executorService = wfThreadcount > 0
                                          ? Executors.newFixedThreadPool(wfThreadcount)
                                          : Executors.newCachedThreadPool();

        Set<EngineWorkflowStepOperation> operations = new HashSet<>();

        executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Building initial state and rules...");

        ruleEngine.addRule(
                //rule to halt processing when flow control says to
                Rules.conditionsRule(
                        Rules.equalsCondition(
                                STEP_ANY_FLOW_CONTROL_HALT_KEY,
                                VALUE_TRUE
                        ),
                        Workflows.getWorkflowEndState()
                )
        );
        ruleEngine.addRule(
                //rule to stop if keepgoing is false and the step fails
                Rules.conditionsRule(
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
                )
        );

        //add any additional strategy rules
        strategyForWorkflow.setup(ruleEngine, executionContext, workflow);

        WorkflowStrategyProfile profile = strategyForWorkflow.getProfile();
        if (profile == null) {
            profile = new SequentialStrategyProfile();
        }

        for (int i = 0; i < stepCount; i++) {
            final int stepNum = executionContext.getStepNumber() + i;
            StepExecutionItem cmd = commands.get(i);

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
            if (null != stepSkipTriggerConditions && stepSkipTriggerConditions.size() > 0) {
                // State that will skip the step
                stepSkipTriggerState = createSkipTriggerStateForStep(stepNum);
                //add a rule to skip the step when its run condition is met, and its skip conditions are met
                ruleEngine.addRule(Rules.conditionsRule(
                        Rules.and(
                                Rules.and(stepStartTriggerConditions),
                                Rules.and(stepSkipTriggerConditions)
                        )
                        ,
                        stepSkipTriggerState
                ));
                executionContext.getExecutionListener().log(
                        Constants.DEBUG_LEVEL,
                        String.format("skip conditions for step [%d]: %s", stepNum, stepSkipTriggerConditions)
                );
            }

            operations.add(new EngineWorkflowStepOperation(
                    stepNum,
                    cmd.getLabel(),
                    callable(cmd, executionContext, stepNum, wlistener, workflow.isKeepgoing()),
                    stepRunTriggerState,
                    stepSkipTriggerState,
                    stepStartTriggerConditions,
                    stepSkipTriggerConditions
            ));
        }

        executionContext.getExecutionListener().log(
                Constants.DEBUG_LEVEL,
                "Create rule engine with rules: " + ruleEngine
        );
        executionContext.getExecutionListener().log(
                Constants.DEBUG_LEVEL,
                "Create workflow engine with state: " + state
        );

        WorkflowSystem workflowEngine = getWorkflowSystemBuilder()
                .ruleEngine(ruleEngine)
                .executor(executorService)
                .state(state)
                .listener(createListener(executionContext.getExecutionListener()))
                .build();

        Set<WorkflowSystem.OperationResult<EngineWorkflowStepOperationSuccess, EngineWorkflowStepOperation>>
                operationResults =
                workflowEngine.processOperations(operations);


        String statusString = null;
        ControlBehavior controlBehavior = null;


        boolean workflowSuccess = !workflowEngine.isInterrupted();
        for (WorkflowSystem.OperationResult<EngineWorkflowStepOperationSuccess, EngineWorkflowStepOperation>
                operationResult : operationResults) {
            EngineWorkflowStepOperationSuccess success = operationResult.getSuccess();
            EngineWorkflowStepOperation operation = operationResult.getOperation();
            if (success != null) {
                stepExecutionResults.put(success.stepNum, success.result);
                if (!success.result.isSuccess()) {
                    stepFailures.put(success.stepNum, success.result);
                }
                stepResults.add(success.result);
                if (success.controlBehavior != null && success.controlBehavior != ControlBehavior.Continue) {
                    controlBehavior = success.controlBehavior;
                    statusString = success.statusString;
                }
            } else {
                workflowSuccess = false;
                Throwable failure = operationResult.getFailure();
                StepFailureReason reason = StepFailureReason.Unknown;

                String message = String.format(
                        "Exception while executing step [%d]: \t[%s]",
                        operation.stepNum,
                        failure.toString()
                );
                if (failure instanceof CancellationException) {
                    reason = StepFailureReason.Interrupted;

                    message = String.format(
                            "Cancellation while running step [%d]",
                            operation.stepNum
                    );
                }
                executionContext.getExecutionListener().log(Constants.ERR_LEVEL, message);
                stepFailures.put(operation.stepNum, StepExecutionResultImpl.wrapStepException(
                        failure instanceof StepException ? (StepException) failure :
                        new StepException(message, failure, reason)
                ));

            }
        }
        for (EngineWorkflowStepOperation operation : operations) {
            if (!operation.isDidRun()) {
                executionContext.getExecutionListener().log(
                        Constants.WARN_LEVEL,
                        String.format(
                                "Step [%d] did not run. start conditions: %s, skip conditions: %s",
                                operation.stepNum,
                                operation.startTriggerConditions,
                                operation.skipTriggerConditions
                        )
                );
            }
        }
        // fail if there is any result is not successful
        Predicate<StepExecutionResult> resultSuccess = new Predicate<StepExecutionResult>() {
            @Override
            public boolean apply(final StepExecutionResult stepExecutionResult) {
                return stepExecutionResult.isSuccess();
            }
        };

        if (FluentIterable.from(stepExecutionResults.values()).anyMatch(Predicates.not(resultSuccess))) {
            workflowSuccess = false;
        }

        WorkflowStatusResult workflowResult = workflowResult(
                workflowSuccess,
                statusString,
                null != controlBehavior ? controlBehavior : ControlBehavior.Continue
        );

        final Map<String, Collection<StepExecutionResult>> nodeFailures = convertFailures(stepFailures);
        return new BaseWorkflowExecutionResult(
                stepResults,
                nodeFailures,
                stepFailures,
                null,
                workflowResult
        );

    }


    private WorkflowSystemEventListener createListener(final ExecutionListener executionListener) {
        return new WorkflowSystemEventListener() {
            @Override
            public void onEvent(final WorkflowSystemEvent event) {
                executionListener.log(Constants.DEBUG_LEVEL, event.getEventType() + ": " + event.getMessage());
            }
        };
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

    Callable<StepResultCapture> callable(
            final StepExecutionItem cmd,
            final StepExecutionContext executionContext,
            final int i,
            final WorkflowExecutionListener wlistener, final boolean keepgoing
    )
    {
        return new Callable<StepResultCapture>() {
            @Override
            public StepResultCapture call() {
                final Map<Integer, StepExecutionResult> stepFailedMap = new HashMap<Integer, StepExecutionResult>();
                List<StepExecutionResult> resultList = new ArrayList<>();
                try {
                    StepResultCapture stepResultCapture = executeWorkflowStep(
                            executionContext,
                            stepFailedMap,
                            resultList,
                            keepgoing,
                            wlistener,
                            i,
                            cmd
                    );
                    return stepResultCapture;
                } catch (Throwable e) {
                    String message = String.format(
                            "Exception while executing step [%d]: [%s]",
                            i,
                            e.toString()
                    );
                    executionContext.getExecutionListener().log(Constants.ERR_LEVEL, message);
                    throw new RuntimeException(e);
                }
            }
        };
    }

}