package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.steps.*;
import com.dtolabs.rundeck.core.rules.*;
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

    public EngineWorkflowExecutor(final Framework framework) {
        super(framework);
    }

    public static String stepKey(final String key, final int stepNum) {
        return key.replace("#", Integer.toString(stepNum));
    }

    public static void updateStateWithStepResultData(
            final MutableStateObj state,
            final int stepNum,
            final Map<String, Object> resultData,
            final Map<String, Object> failureData
    )
    {
        HashMap<String, String> stringStringHashMap = new HashMap<>();
        if (null != resultData) {
            for (String s : resultData.keySet()) {
                stringStringHashMap.put(
                        stepKey(STEP_DATA_RESULT_KEY_PREFIX + s, stepNum),
                        resultData.get(s).toString()
                );
            }
        }
        if (null != failureData) {
            for (String s : failureData.keySet()) {
                stringStringHashMap.put(
                        stepKey(STEP_DATA_RESULT_KEY_PREFIX + s, stepNum),
                        resultData.get(s).toString()
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
            return null;
        }

    }


    @Override
    public WorkflowExecutionResult executeWorkflowImpl(
            final StepExecutionContext executionContext,
            final WorkflowExecutionItem item
    )
    {
        executionContext.getExecutionListener().log(Constants.DEBUG_LEVEL, "Start ConditionalWorkflowStrategy2");
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
            strategyForWorkflow = framework.getWorkflowStrategyService().getStrategyForWorkflow(
                    item,
                    workflow.getStrategyConfig()
            );
        } catch (ExecutionServiceException e) {
            executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Exception: " + e.getClass() + ": " + e
                    .getMessage());
            return new BaseWorkflowExecutionResult(
                    stepResults,
                    new HashMap<String, Collection<StepExecutionResult>>(),
                    stepFailures,
                    null,
                    WorkflowResultFailed
            );
        }
        //getProfile(strategy);

        RuleEngine ruleEngine = Rules.createEngine();

        MutableStateObj mutable = States.mutable(DataContextUtils.flattenDataContext(
                executionContext.getDataContext()));
        mutable.updateState(WORKFLOW_KEEPGOING_KEY, Boolean.toString(workflow.isKeepgoing()));
        MutableStateObj state = new StateLogger(
                mutable,
                executionContext.getExecutionListener()
        );

        int wfThreadcount = strategyForWorkflow.getThreadCount();//strategy.equals("parallel") ? 0 : workflow
        // .getThreadcount();

        ExecutorService executorService = wfThreadcount > 0
                                          ? Executors.newFixedThreadPool(wfThreadcount)
                                          : Executors.newCachedThreadPool();

        Set<StepOperation> operations = new HashSet<>();

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
                        set(
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
        strategyForWorkflow.setup(ruleEngine);

        WorkflowStrategyProfile profile = strategyForWorkflow.getProfile();

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
                //add a rule to skip the step when conditions are met
                ruleEngine.addRule(Rules.conditionsRule(stepSkipTriggerConditions, stepSkipTriggerState));
                executionContext.getExecutionListener().log(
                        Constants.DEBUG_LEVEL,
                        String.format("skip conditions for step [%d]: %s", stepNum, stepSkipTriggerConditions)
                );
            }

            operations.add(new StepOperation(
                    stepNum,
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

        WorkflowEngine workflowEngine = new WorkflowEngine(ruleEngine, state, executorService);
        workflowEngine.setAppLogger(executionContext.getExecutionListener());
        Set<WorkflowSystem.OperationResult<StepSuccess, StepOperation>> operationResults =
                workflowEngine.processOperations(operations);

        for (WorkflowSystem.OperationResult<StepSuccess, StepOperation> operationResult : operationResults) {
            StepSuccess success = operationResult.getSuccess();
            StepOperation operation = operationResult.getOperation();
            if (success != null) {
                stepExecutionResults.put(success.stepNum, success.result);
                if (!success.result.isSuccess()) {
                    stepFailures.put(success.stepNum, success.result);
                }
                stepResults.add(success.result);
            } else {
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
        for (StepOperation operation : operations) {
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

        WorkflowStatusResult workflowResult = null;
        // Poll results, fail if there is any result is missing or not successful
        for (int i = 0; i < stepCount; i++) {
            int stepNum = i + executionContext.getStepNumber();
            if (null != stepExecutionResults.get(stepNum) &&
                !stepExecutionResults.get(stepNum).isSuccess()) {
//                logger.debug("No result, or failure result for step " +
//                                   stepNum +
//                                   ", failing workflow: " +
//                                   stepExecutionResults.get(stepNum));
                workflowResult = WorkflowResultFailed;
            }
        }
        if (null == workflowResult) {
            workflowResult = workflowResult(true, null, ControlBehavior.Continue);
        }
//        final Exception orig = exception;
        final Map<String, Collection<StepExecutionResult>> nodeFailures = convertFailures(stepFailures);
        return new BaseWorkflowExecutionResult(
                stepResults,
                nodeFailures,
                stepFailures,
                null,
                workflowResult
        );

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

    static private Set<Condition> set(final Condition... condition) {
        HashSet<Condition> conditions = new HashSet<>();
        conditions.addAll(Arrays.asList(condition));
        return conditions;
    }


    private void addConditionalRules(final StepExecutionItem cmd, int stepNum, final RuleEngine ruleEngine) {
        if (cmd instanceof ConditionalStepExecutionItem) {
            ConditionalStepExecutionItem conds = (ConditionalStepExecutionItem) cmd;
            Map<String, Object> conditionsMap = conds.getConditionsMap();
            addConditionalRules(conditionsMap, stepNum, ruleEngine);
        }
    }

    private void addConditionalRules(
            final Map<String, Object> conditionsMap,
            final int stepNum,
            final RuleEngine ruleEngine
    )
    {
        logger.debug("add conditions from step for " + conditionsMap);
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

    /**
     * operation for running a step
     */
    static class StepOperation implements WorkflowSystem.Operation<StepSuccess> {
        int stepNum;
        Set<Condition> startTriggerConditions;
        Set<Condition> skipTriggerConditions;
        Callable<StepResultCapture> callable;
        private StateObj startTriggerState;
        private StateObj skipTriggerState;
        private boolean didRun = false;

        public StepOperation(
                final int stepNum,
                final Callable<StepResultCapture> callable,
                final StateObj startTriggerState,
                final StateObj skipTriggerState,
                final Set<Condition> startTriggerConditions,
                final Set<Condition> skipTriggerConditions
        )
        {
            this.stepNum = stepNum;
            this.callable = callable;
            this.startTriggerState = startTriggerState;
            this.startTriggerConditions = startTriggerConditions;
            this.skipTriggerConditions = skipTriggerConditions;
            this.skipTriggerState = skipTriggerState;
        }

        @Override
        public boolean shouldRun(final StateObj state) {
            return state.hasState(startTriggerState);
        }

        @Override
        public boolean shouldSkip(final StateObj state) {
            return null != skipTriggerState && state.hasState(skipTriggerState);
        }

        @Override
        public StepSuccess call() throws Exception {
            didRun = true;
            StepResultCapture stepResultCapture = callable.call();
            StepExecutionResult result = stepResultCapture.getStepResult();
            ControlBehavior controlBehavior = stepResultCapture.getControlBehavior();
            String statusString = stepResultCapture.getStatusString();

            logger.debug("StepOperation callable complete: " + stepResultCapture);

            MutableStateObj stateChanges = States.mutable();
            boolean success = null != result && result.isSuccess();
            if (result != null) {
                updateStateWithStepResultData(
                        stateChanges,
                        stepNum,
                        result.getResultData(),
                        result.getFailureData()
                );
            }
            stateChanges.updateState(stepKey(STEP_COMPLETED_KEY, stepNum), VALUE_TRUE);
            String stepResultValue = success ? STEP_STATE_RESULT_SUCCESS : STEP_STATE_RESULT_FAILURE;
            stateChanges.updateState(stepKey(STEP_STATE_KEY, stepNum), stepResultValue);
            if (success) {
                stateChanges.updateState(STEP_ANY_STATE_SUCCESS_KEY, VALUE_TRUE);
            } else {
                stateChanges.updateState(STEP_ANY_STATE_FAILED_KEY, VALUE_TRUE);
            }
            stateChanges.updateState(stepKey(STEP_BEFORE_KEY, stepNum), VALUE_FALSE);
            stateChanges.updateState(stepKey(STEP_AFTER_KEY, stepNum), VALUE_TRUE);

            if (controlBehavior != null) {
                stateChanges.updateState(stepKey(STEP_FLOW_CONTROL_KEY, stepNum), controlBehavior.toString());
                if (controlBehavior == ControlBehavior.Halt) {
                    stateChanges.updateState(STEP_ANY_FLOW_CONTROL_HALT_KEY, VALUE_TRUE);
                }
                if (null != statusString) {
                    stateChanges.updateState(stepKey(STEP_FLOW_CONTROL_STATUS_KEY, stepNum), statusString);
                }
            }

            return new StepSuccess(stepNum, result, stateChanges);
        }

        @Override
        public StateObj getSkipState(final StateObj state) {
            MutableStateObj stateChanges = States.mutable();
            stateChanges.updateState(stepKey(STEP_COMPLETED_KEY, stepNum), VALUE_FALSE);
            stateChanges.updateState(stepKey(STEP_STATE_KEY, stepNum), STEP_STATE_RESULT_SKIPPED);
            stateChanges.updateState(STEP_ANY_STATE_SKIPPED_KEY, VALUE_TRUE);
            stateChanges.updateState(stepKey(STEP_BEFORE_KEY, stepNum), VALUE_FALSE);
            stateChanges.updateState(stepKey(STEP_AFTER_KEY, stepNum), VALUE_TRUE);
            return stateChanges;
        }

        @Override
        public StateObj getFailureState(final Throwable t) {
            MutableStateObj stateChanges = States.mutable();
            stateChanges.updateState(stepKey(STEP_COMPLETED_KEY, stepNum), VALUE_TRUE);
            stateChanges.updateState(
                    stepKey(STEP_STATE_KEY, stepNum),
                    STEP_STATE_RESULT_FAILURE
            );
            stateChanges.updateState(stepKey(STEP_BEFORE_KEY, stepNum), VALUE_FALSE);
            stateChanges.updateState(stepKey(STEP_AFTER_KEY, stepNum), VALUE_TRUE);
            return stateChanges;
        }

        public boolean isDidRun() {
            return didRun;
        }
    }

    static class StepSuccess implements WorkflowSystem.OperationSuccess {
        int stepNum;
        StepExecutionResult result;
        StateObj newState;

        public StepSuccess(final int stepNum, final StepExecutionResult result, final StateObj newState) {
            this.stepNum = stepNum;
            this.result = result;
            this.newState = newState;
        }

        @Override
        public StateObj getNewState() {
            return newState;
        }

        @Override
        public String toString() {
            return "StepSuccess{" +
                   "stepNum=" + stepNum +
                   ", result=" + result +
                   ", newState=" + newState +
                   '}';
        }
    }

}