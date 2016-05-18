package com.dtolabs.rundeck.core.execution.workflow

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.rules.Condition
import com.dtolabs.rundeck.core.rules.StateObj
import com.dtolabs.rundeck.core.rules.WorkflowEngineBuilder
import com.dtolabs.rundeck.core.rules.WorkflowSystem
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

import java.util.concurrent.CancellationException

/**
 * Created by greg on 5/18/16.
 */
class EngineWorkflowExecutorSpec extends Specification {
    public static final String PROJECT_NAME = 'EngineWorkflowExecutorSpec'
    Framework framework
    FrameworkProject testProject

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
    }

    def teardown() {
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }

    def "basic success"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        framework.getStepExecutionService().registerInstance('blah', Mock(StepExecutor) {
            executeWorkflowStep(*_) >> new StepExecutionResultImpl()
        }
        )
        framework.getWorkflowStrategyService().registerInstance('test-strategy', Mock(WorkflowStrategy))

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Stub(ExecutionListener)
            getFrameworkProject() >> PROJECT_NAME
        }
        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        }
                ]
                getStrategy() >> 'test-strategy'
            }
        }


        when:
        def result = engine.executeWorkflowImpl(context, item)

        then:
        null != result
        result.success

    }

    class SkipProfile extends SequentialStrategyProfile {
        @Override
        Set<Condition> getSkipConditionsForStep(
                final WorkflowExecutionItem item,
                final int stepNum,
                final boolean isFirstStep
        )
        {
            [
                    { StateObj state ->
                        true
                    } as Condition

            ] as Set
        }
    }

    def "basic skip"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        framework.getStepExecutionService().registerInstance('blah', Mock(StepExecutor) {
            executeWorkflowStep(*_) >> new StepExecutionResultImpl()
        }
        )
        framework.getWorkflowStrategyService().registerInstance('test-strategy', Mock(WorkflowStrategy) {
            getProfile() >> new SkipProfile()
        }
        )

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Stub(ExecutionListener)
            getFrameworkProject() >> PROJECT_NAME
        }
        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        }
                ]
                getStrategy() >> 'test-strategy'
            }
        }


        when:
        def result = engine.executeWorkflowImpl(context, item)

        then:
        null != result
        result.success
        result.resultSet.size() == 0

    }

    static class MyFailure implements FailureReason {
        String message;

        MyFailure(final String message) {
            this.message = message
        }

        @Override
        String toString() {
            message
        }
    }

    def "basic failure"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        def reason = new MyFailure("test failure")
        framework.getStepExecutionService().registerInstance('blah', Mock(StepExecutor) {
            executeWorkflowStep(*_) >> new StepExecutionResultImpl(null, reason, "a failure")
        }
        )
        framework.getWorkflowStrategyService().registerInstance('test-strategy', Mock(WorkflowStrategy))

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Stub(ExecutionListener)
            getFrameworkProject() >> PROJECT_NAME
        }
        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        }
                ]
                getStrategy() >> 'test-strategy'
            }
        }


        when:
        def result = engine.executeWorkflowImpl(context, item)

        then:
        null != result
        !result.success
        !result.stepFailures[0].success
        result.stepFailures[0].failureReason == reason
        result.stepFailures[0].failureMessage == 'a failure'
    }

    class MyEngineBuilder extends WorkflowEngineBuilder {
        WorkflowSystem built;

        @Override
        WorkflowSystem build() {
            this.built = super.build()
            return built
        }
    }

    def "basic abort"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        def builder = new MyEngineBuilder()
        engine.setWorkflowSystemBuilder(builder)
        framework.getStepExecutionService().registerInstance(
                'blah',
                Mock(StepExecutor) {
                    executeWorkflowStep(*_) >> { args ->
                        //simulate interruption of the thread running the executor
                        builder.built.interrupted = true
                        new StepExecutionResultImpl()
                    }
                }
        )
        framework.getStepExecutionService().registerInstance(
                'blah2',
                Mock(StepExecutor) {
                    executeWorkflowStep(*_) >> { args ->
                        //will start after step 1
                        println('-> 2 Starting...')
                        //sleep to allow engine to cycle passed 30s sleep time and notice interruption
                        Thread.sleep(2000)
                        println('-> 2 Finishing...')
                        new StepExecutionResultImpl()
                    }
                }
        )
        framework.getWorkflowStrategyService().registerInstance('test-strategy', Mock(WorkflowStrategy))

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Mock(ExecutionListener) {
                log(_, _) >> {
                    println(it[1])
                }
            }
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 1
        }
        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        },
                        Mock(StepExecutionItem) {
                            getType() >> 'blah2'
                        }
                ]
                getStrategy() >> 'test-strategy'
            }
        }


        when:
        def result = engine.executeWorkflowImpl(context, item)

        then:
        null != result
        !result.success
        result.stepFailures.size() == 1
        result.stepFailures.keySet() == [2] as Set
        result.stepFailures[2].failureReason == StepFailureReason.Interrupted
        result.stepFailures[2].failureMessage == 'Cancellation while running step [2]'
    }
}
