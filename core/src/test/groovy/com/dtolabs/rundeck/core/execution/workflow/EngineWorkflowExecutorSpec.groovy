package com.dtolabs.rundeck.core.execution.workflow

import com.dtolabs.rundeck.core.NodesetEmptyException
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.INodeEntry
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionContextImpl
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.ExecutionListenerOverride
import com.dtolabs.rundeck.core.execution.FailedNodesListener
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.dispatch.Dispatchable
import com.dtolabs.rundeck.core.execution.dispatch.DispatcherResult
import com.dtolabs.rundeck.core.execution.service.NodeExecutorResult
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResult
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutionResultImpl
import com.dtolabs.rundeck.core.execution.workflow.steps.StepExecutor
import com.dtolabs.rundeck.core.execution.workflow.steps.StepFailureReason
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.core.rules.Condition
import com.dtolabs.rundeck.core.rules.RuleEngine
import com.dtolabs.rundeck.core.rules.Rules
import com.dtolabs.rundeck.core.rules.StateObj
import com.dtolabs.rundeck.core.rules.WorkflowEngineBuilder
import com.dtolabs.rundeck.core.rules.WorkflowSystem
import com.dtolabs.rundeck.core.rules.Workflows
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    static class TestWorkflowStrategy implements WorkflowStrategy {
        @Override
        int getThreadCount() {
            return 0
        }

        @Override
        void setup(final RuleEngine ruleEngine, final StepExecutionContext context, final IWorkflow workflow) {

        }

        @Override
        Validator.Report validate(final IWorkflow workflow) {
            return null
        }

        @Override
        WorkflowStrategyProfile getProfile() {
            return null
        }
    }

    static class TestSuccessStepExecutor implements StepExecutor {
        @Override
        boolean isNodeDispatchStep(final StepExecutionItem item) {
            return false
        }

        @Override
        StepExecutionResult executeWorkflowStep(
            final StepExecutionContext executionContext,
            final StepExecutionItem item
        ) throws StepException {
            new StepExecutionResultImpl()
        }
    }
    def "basic success"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        framework.getStepExecutionService().registerClass('blah', TestSuccessStepExecutor)
        framework.getWorkflowStrategyService().registerClass('test-strategy', TestWorkflowStrategy)

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Mock(ExecutionListener){
                createOverride()>>Mock(ExecutionListenerOverride)
            }
            getNodes() >> Mock(INodeSet){
                getNodes() >> Arrays.asList(new NodeEntryImpl("set1node1"))
            }
            getWorkflowExecutionListener() >> new NoopWorkflowExecutionListener()
            getFrameworkProject() >> PROJECT_NAME
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            useSingleComponentOfType(_) >> Optional.empty()
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

    def "basic success on empty node filter"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        framework.getStepExecutionService().registerClass('blah', TestSuccessStepExecutor)
        framework.getWorkflowStrategyService().registerClass('test-strategy', TestWorkflowStrategy)

        Map dataContext = new HashMap<String, Map<String, String>>()
        Map nodeSettings = new HashMap<String, String>()
        nodeSettings.put("successOnEmptyNodeFilter", "true")
        dataContext.put("job", nodeSettings)

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Mock(ExecutionListener){
                createOverride()>>Mock(ExecutionListenerOverride)
            }
            getNodes() >> Mock(INodeSet){
                getNodes() >> Arrays.asList()
            }
            getWorkflowExecutionListener() >> new NoopWorkflowExecutionListener()
            getFrameworkProject() >> PROJECT_NAME
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            useSingleComponentOfType(_) >> Optional.empty()
            getDataContext() >> dataContext
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

    def "basic fail on empty node filter"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        framework.getStepExecutionService().registerClass('blah', TestSuccessStepExecutor)
        framework.getWorkflowStrategyService().registerClass('test-strategy', TestWorkflowStrategy)

        Map dataContext = new HashMap<String, Map<String, String>>()
        Map nodeSettings = new HashMap<String, String>()
        nodeSettings.put("successOnEmptyNodeFilter", "false")
        dataContext.put("job", nodeSettings)

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Mock(ExecutionListener){
                createOverride()>>Mock(ExecutionListenerOverride)
            }
            getNodes() >> Mock(INodeSet){
                getNodes() >> Arrays.asList()
            }
            getWorkflowExecutionListener() >> new NoopWorkflowExecutionListener()
            getFrameworkProject() >> PROJECT_NAME
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            useSingleComponentOfType(_) >> Optional.empty()
            getDataContext() >> dataContext
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
        result.exception instanceof NodesetEmptyException
        result.exception.message.contains("No matched nodes")

    }

    static class SkipProfile extends SequentialStrategyProfile {
        Map<Integer, Set<Condition>> skipConditions = [:]

        SkipProfile(final Map<Integer, Set<Condition>> skipConditions) {
            this.skipConditions = skipConditions
        }

        @Override
        Set<Condition> getSkipConditionsForStep(
                final WorkflowExecutionItem item,
                final int stepNum,
                final boolean isFirstStep
        )
        {
            skipConditions[stepNum]
        }
    }

    static class TestSkipProfileWorkflowStrategy extends TestWorkflowStrategy {
        @Override
        WorkflowStrategyProfile getProfile() {
            new SkipProfile([(1): [({ StateObj state -> true } as Condition)] as Set])
        }
    }

    def "basic skip"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)

        framework.getStepExecutionService().registerClass('blah', TestSuccessStepExecutor)
        framework.getWorkflowStrategyService().registerClass('test-skip-strategy', TestSkipProfileWorkflowStrategy)

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Stub(ExecutionListener)
            getNodes() >> Mock(INodeSet){
                getNodes() >> Arrays.asList(new NodeEntryImpl("set1node1"))
            }
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 1
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            useSingleComponentOfType(_) >> Optional.empty()
        }
        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        }
                ]
                getStrategy() >> 'test-skip-strategy'
            }
        }


        when:
        def result = engine.executeWorkflowImpl(context, item)

        then:
        null != result
        result.success
        result.resultSet.size() == 0

    }

    static final class TestSkip3NotFailedWorkflowStrategy extends TestWorkflowStrategy {
        @Override
        WorkflowStrategyProfile getProfile() {
            new SkipProfile(
                [(3): Rules.conditionSet(
                    Condition.not(
                        Rules.equalsCondition('step.1.state', 'failed')
                    )
                )]
            )
        }
    }

    def "skip after success"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)

        framework.getStepExecutionService().registerClass('blah', TestSuccessStepExecutor)
        framework.getStepExecutionService().registerClass('blah2', TestSuccessStepExecutor)
        framework.getWorkflowStrategyService().
            registerClass('test-skip-3-not-strategy', TestSkipProfileWorkflowStrategy)


        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Stub(ExecutionListener)
            getNodes() >> Mock(INodeSet){
                getNodes() >> Arrays.asList(new NodeEntryImpl("set1node1"))
            }
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 1
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            useSingleComponentOfType(_) >> Optional.empty()
        }
        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        },
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        },
                        Mock(StepExecutionItem) {
                            getType() >> 'blah2'
                        }
                ]
                getStrategy() >> 'test-skip-3-not-strategy'
            }
        }


        when:
        def result = engine.executeWorkflowImpl(context, item)

        then:
        null != result
        result.success
        result.resultSet.size() == 2

    }

    static final class TestSkip3NotBothWorkflowStrategy extends TestWorkflowStrategy {
        @Override
        WorkflowStrategyProfile getProfile() {
            new SkipProfile(
                (3): Rules.conditionSet(
                    Condition.not(
                        Condition.and(
                            Rules.equalsCondition('step.1.state', 'success'),
                            Rules.equalsCondition('step.2.state', 'success')
                        )
                    )
                )
            )
        }
    }
    /**
     * skip conditions will be generated as NOT (run conditions),
     * verify that skip condition does not create skip state prior to
     * the time the step should actually be evaluated
     * @return
     */
    def "run after 2 success"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)

        framework.getStepExecutionService().registerClass('blah', TestSuccessStepExecutor)
        framework.getStepExecutionService().registerClass('blah2', TestSuccessStepExecutor)
        framework.getWorkflowStrategyService().
            registerClass('test-skip-3-not-both-strategy', TestSkip3NotBothWorkflowStrategy)



        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Stub(ExecutionListener)
            getNodes() >> Mock(INodeSet){
                getNodes() >> Arrays.asList(new NodeEntryImpl("set1node1"))
            }
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 1
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            useSingleComponentOfType(_) >> Optional.empty()
        }
        def item = Mock(WorkflowExecutionItem) {
            getWorkflow() >> Mock(IWorkflow) {
                getCommands() >> [
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        },
                        Mock(StepExecutionItem) {
                            getType() >> 'blah'
                        },
                        Mock(StepExecutionItem) {
                            getType() >> 'blah2'
                        }
                ]
                getStrategy() >> 'test-skip-3-not-both-strategy'
            }
        }


        when:
        def result = engine.executeWorkflowImpl(context, item)

        then:
        null != result
        result.success
        result.resultSet.size() == 3

    }

    static final class TestSkip2NotSuccess1WorkflowStrategy extends TestWorkflowStrategy {
        @Override
        WorkflowStrategyProfile getProfile() {
            new SkipProfile(
                (2): Rules.conditionSet(
                    Condition.not(
                        Rules.equalsCondition('step.1.state', 'success')
                    )
                )
            )
        }
    }


    def "don't skip after success"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        framework.getStepExecutionService().registerClass('blah', TestSuccessStepExecutor)
        framework.getStepExecutionService().registerClass('blah2', TestSuccessStepExecutor)
        framework.getWorkflowStrategyService().
            registerClass('test-skip-2-not-success-1-strategy', TestSkip2NotSuccess1WorkflowStrategy)

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Stub(ExecutionListener)
            getNodes() >> Mock(INodeSet){
                getNodes() >> Arrays.asList(new NodeEntryImpl("set1node1"))
            }
            getFrameworkProject() >> PROJECT_NAME
            getStepNumber() >> 1
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            useSingleComponentOfType(_) >> Optional.empty()
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
                getStrategy() >> 'test-skip-2-not-success-1-strategy'
                isKeepgoing() >> true
            }
        }


        when:
        def result = engine.executeWorkflowImpl(context, item)

        then:
        null != result
        result.success
        result.resultSet.size() == 2

    }

    static class MyReason implements FailureReason {
        String message;

        MyReason(final String message) {
            this.message = message
        }

        @Override
        String toString() {
            message
        }
    }

    static class TestBasicFailStepExecutor implements StepExecutor {
        @Override
        boolean isNodeDispatchStep(final StepExecutionItem item) {
            return false
        }

        @Override
        StepExecutionResult executeWorkflowStep(
            final StepExecutionContext executionContext,
            final StepExecutionItem item
        ) throws StepException {
            new StepExecutionResultImpl(null, new MyReason("test failure"), "a failure")
        }
    }
    def "basic failure"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        framework.getStepExecutionService().registerClass('blah',TestBasicFailStepExecutor)
        framework.getWorkflowStrategyService().registerClass('test-strategy', TestWorkflowStrategy)

        def context = Mock(StepExecutionContext) {
            getExecutionListener() >> Stub(ExecutionListener)
            getNodes() >> Mock(INodeSet){
                getNodes() >> Arrays.asList(new NodeEntryImpl("set1node1"))
            }
            getFrameworkProject() >> PROJECT_NAME
            getFramework() >> framework
            componentForType(_) >> Optional.empty()
            componentsForType(_) >> []
            useSingleComponentOfType(_) >> Optional.empty()
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
        result.stepFailures[0].failureReason.toString() == 'test failure'
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

    class LogListener implements ExecutionListener {
        @Override public void ignoreErrors(boolean ignore){}

        @Override
        void log(final int level, final String message) {
            println(message)
        }

        @Override
        void log(final int level, final String message, final Map eventMeta) {
            println(message)
        }

        @Override
        void event(final String eventType, final String message, final Map eventMeta) {

        }

        @Override
        FailedNodesListener getFailedNodesListener() {
            return null
        }

        @Override
        void beginNodeExecution(final ExecutionContext context, final String[] command, final INodeEntry node) {

        }

        @Override
        void finishNodeExecution(
                final NodeExecutorResult result,
                final ExecutionContext context,
                final String[] command,
                final INodeEntry node
        )
        {

        }

        @Override
        void beginNodeDispatch(final ExecutionContext context, final StepExecutionItem item) {

        }

        @Override
        void beginNodeDispatch(final ExecutionContext context, final Dispatchable item) {

        }

        @Override
        void finishNodeDispatch(
                final DispatcherResult result,
                final ExecutionContext context,
                final StepExecutionItem item
        )
        {

        }

        @Override
        void finishNodeDispatch(
                final DispatcherResult result,
                final ExecutionContext context,
                final Dispatchable item
        )
        {

        }

        @Override
        void beginFileCopyFileStream(final ExecutionContext context, final InputStream input, final INodeEntry node) {

        }

        @Override
        void beginFileCopyFile(final ExecutionContext context, final File input, final INodeEntry node) {

        }

        @Override
        void beginFileCopyScriptContent(final ExecutionContext context, final String input, final INodeEntry node) {

        }

        @Override
        void finishFileCopy(final String result, final ExecutionContext context, final INodeEntry node) {

        }

        @Override
        public void finishMultiFileCopy(String[] result, ExecutionContext context, INodeEntry node) {
        }

        @Override
        public void beginFileCopyFile(ExecutionContext context, List<File> input, INodeEntry node) {
        }

        @Override
        ExecutionListenerOverride createOverride() {
            return null
        }
    }
    class LogListenerThrowingException extends LogListener {
        @Override
        void log(final int level, final String message) {
            if(message.contains("OperationFailed")){
                throw new IllegalArgumentException("Some configuration issue");
            } else {
                super.log(level, message)
            }
        }
    }

    static CountDownLatch TestCountDownLatch =new CountDownLatch(1)
    static class TestAbortStepExecutor implements StepExecutor{
        @Override
        boolean isNodeDispatchStep(final StepExecutionItem item) {
            return false
        }

        @Override
        StepExecutionResult executeWorkflowStep(
            final StepExecutionContext executionContext,
            final StepExecutionItem item
        ) throws StepException {
            //will start after step 1
            println('-> 2 Starting...')
            //trigger thread interrupt
            TestCountDownLatch.countDown()
            try {
                Thread.sleep(20000)
                println('-> 2 Finishing...')
                new StepExecutionResultImpl()
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt()
                throw new StepException("interrupted", e, new MyReason("test"))
            }
        }
    }
    def "basic abort"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        def builder = new MyEngineBuilder()
        engine.setWorkflowSystemBuilderSupplier({->builder})
        TestCountDownLatch = new CountDownLatch(1)

        framework.getStepExecutionService().registerClass('blah', TestSuccessStepExecutor)
        framework.getStepExecutionService().registerClass('blah2', TestAbortStepExecutor)
        framework.getWorkflowStrategyService().registerClass('test-strategy', TestWorkflowStrategy)
        def logger = new LogListener()
        def nodeSet = Mock(INodeSet){
            getNodes() >> Arrays.asList(new NodeEntryImpl("set1node1"))
        }
        def context = ExecutionContextImpl.builder().
                executionListener(logger).
                workflowExecutionListener(new NoopWorkflowExecutionListener()).
                frameworkProject(PROJECT_NAME).
                stepNumber(1)
                .nodes(nodeSet).
                framework(framework).
                build()
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
        def result
        def t = new Thread({
            result = engine.executeWorkflowImpl(context, item)
            println("finished execute workflow")
        }
        )

        new Thread({
            TestCountDownLatch.await(20, TimeUnit.SECONDS)
            println "causing interrupt..."
            t.interrupt()
        }
        ).start()
        t.start()
        t.join()

        then:
        null != result
        result.stepFailures.size() == 1
        result.stepFailures.keySet() == [2] as Set
        result.stepFailures[2].failureReason == StepFailureReason.Interrupted
        result.stepFailures[2].failureMessage == 'Cancellation while running step [2]'
        !result.success

    }

    def "abort and throwing an exception on log event"() {
        given:
        def engine = new EngineWorkflowExecutor(framework)
        def builder = new MyEngineBuilder()
        engine.setWorkflowSystemBuilderSupplier({->builder})
        TestCountDownLatch = new CountDownLatch(1)

        framework.getStepExecutionService().registerClass('blah', TestAbortStepExecutor)
        framework.getWorkflowStrategyService().registerClass('test-strategy', TestWorkflowStrategy)

        def logger = new LogListenerThrowingException()
        def nodeSet = Mock(INodeSet){
            getNodes() >> Arrays.asList(new NodeEntryImpl("set1node1"))
        }

        def context = ExecutionContextImpl.builder().
                executionListener(logger).
                workflowExecutionListener(new NoopWorkflowExecutionListener()).
                frameworkProject(PROJECT_NAME).
                stepNumber(1).
                nodes(nodeSet).
                framework(framework).
                build()
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
        def result
        def t = new Thread({
            result = engine.executeWorkflowImpl(context, item)
            println("finished execute workflow")
        }
        )
        new Thread({
            TestCountDownLatch.await(20, TimeUnit.SECONDS)
            println "causing interrupt..."
            t.interrupt()
        }
        ).start()
        t.start()
        t.join()

        then:
        null != result
        !result.success
        result.stepFailures
        result.stepFailures.size() == 1
    }

    def "default augmentor initial state should not include shared data in state"(){
        given:
            def sut = new EngineWorkflowExecutor.DefaultAugmentor()
            def item = Mock(WorkflowExecutionItem){
                getWorkflow()>>Mock(IWorkflow){
                    isKeepgoing()>>keepgoing
                }
            }
            def context = Mock(StepExecutionContext)
        when:
            def result=sut.getInitialState(item,context)
        then:
            result.getState().size()==2
            result.getState().get(Workflows.WORKFLOW_STATE_ID_KEY)!= null
            result.getState().get(EngineWorkflowExecutor.WORKFLOW_KEEPGOING_KEY)==keepgoing.toString()

        where:
            keepgoing<<[true,false]

    }
}
