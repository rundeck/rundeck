package com.dtolabs.rundeck.core.execution.workflow;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.dispatcher.*;
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl;
import com.dtolabs.rundeck.core.execution.StepExecutionItem;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl;
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason;
import com.dtolabs.rundeck.core.execution.workflow.steps.node.NodeExecutionContext;
import com.dtolabs.rundeck.core.rules.*;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executors;
import java.util.function.Function;


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
                    new HashMap<>(),
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


        DataContext base = new BaseDataContext();
        final MultiDataContextImpl<String, DataContext> sharedContext = MultiDataContextImpl.withBase(base);

        WorkflowSystem.SharedData<MultiDataContext<String, DataContext>> dataContextSharedData
                = new WorkflowSystem.SharedData<MultiDataContext<String, DataContext>>() {
            @Override
            public void addData(final MultiDataContext<String,DataContext> item) {
                if(item!=null) {
                    sharedContext.merge(item);
                }
            }

            @Override
            public MultiDataContext<String,DataContext> produceNext() {
                MultiDataContextImpl<String, DataContext> next = new MultiDataContextImpl<>();
                next.merge(sharedContext);
                return next;
            }
        };
        Set<WorkflowSystem.OperationResult<MultiDataContext<String,DataContext>, EngineWorkflowStepOperationCompleted,
                EngineWorkflowStepOperation>>
                operationResults =
                workflowEngine.processOperations(operations, dataContextSharedData);


        String statusString = null;
        ControlBehavior controlBehavior = null;


        boolean workflowSuccess = !workflowEngine.isInterrupted();
        for (WorkflowSystem.OperationResult<MultiDataContext<String,DataContext>, EngineWorkflowStepOperationCompleted,
                EngineWorkflowStepOperation> operationResult : operationResults) {
            EngineWorkflowStepOperationCompleted success = operationResult.getSuccess();
            EngineWorkflowStepOperation operation = operationResult.getOperation();
            Throwable failure = operationResult.getFailure();

            if (success != null) {
                StepResultCapture result = success.getStepResultCapture();
                stepExecutionResults.put(success.stepNum, result.getStepResult());
                if (!result.getStepResult().isSuccess()) {
                    stepFailures.put(success.stepNum, result.getStepResult());
                    workflowSuccess = false;
                }
                stepResults.add(result.getStepResult());
                if (result.getControlBehavior() != null && result.getControlBehavior() != ControlBehavior.Continue) {
                    controlBehavior = result.getControlBehavior();
                    statusString = result.getStatusString();
                }
                System.out.println("Step result data: "+result.getResultData());//XXX
            } else {
                workflowSuccess = false;
                StepFailureReason reason = StepFailureReason.Unknown;

                String message = String.format(
                        "Exception while executing step [%d]: \t[%s]",
                        operation.stepNum,
                        failure.toString()
                );
                failure.printStackTrace(System.out);//XXX
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

    Function<MultiDataContext<String,DataContext>, StepResultCapture> callable(
            final StepExecutionItem cmd,
            final StepExecutionContext executionContext,
            final int i,
            final WorkflowExecutionListener wlistener,
            final boolean keepgoing
    )
    {
        return inputData -> {
            final Map<Integer, StepExecutionResult> stepFailedMap = new HashMap<>();
            List<StepExecutionResult> resultList = new ArrayList<>();

            BaseDataContext newDataContext = new BaseDataContext();
            newDataContext.merge(DataContextUtils.context(executionContext.getDataContext()));
            if(null!=inputData.getBase()) {
                newDataContext.merge(inputData.getBase());
            }
            //TODO: merge node results from input multidata

            HashMap<String, Map<String,Map<String,String>>> newNodeData = new HashMap<>();

            Map<String, DataContext> inputNodeData = inputData.getData();
            if(executionContext instanceof NodeExecutionContext){
                NodeExecutionContext nctx = (NodeExecutionContext) executionContext;
                Map<String, Map<String, Map<String, String>>> nodeDataContext = nctx.getNodeDataContext();
                for (String node : nodeDataContext.keySet()) {
                    BaseDataContext d = new BaseDataContext();
                    d.merge(DataContextUtils.context(nodeDataContext.get(node)));
                    DataContext dataContext = inputNodeData.get(node);
                    if(null!=dataContext){
                        d.merge(dataContext);
                    }
                    newNodeData.put(node, d);
                }
            } else if (inputNodeData != null) {
                newNodeData.putAll(inputNodeData);
            }


            executionContext.getExecutionListener().log(Constants.ERR_LEVEL, "Input data context: " + inputData);
            StepExecutionContext newContext =
                    ExecutionContextImpl.builder(executionContext)
                                        .dataContext(newDataContext)
                                        .nodeDataContext(newNodeData)
                                        .build();
            try {
                return executeWorkflowStep(
                        newContext,
                        stepFailedMap,
                        resultList,
                        keepgoing,
                        wlistener,
                        i,
                        cmd
                );
            } catch (Throwable e) {
                String message = String.format(
                        "Exception while executing step [%d]: [%s]",
                        i,
                        e.toString()
                );
                executionContext.getExecutionListener().log(Constants.ERR_LEVEL, message);
                throw new RuntimeException(e);
            }
        };
    }

}