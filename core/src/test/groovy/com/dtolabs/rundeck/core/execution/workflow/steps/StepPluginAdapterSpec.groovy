package com.dtolabs.rundeck.core.execution.workflow.steps

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.common.PropertyRetriever
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.StepExecutionItem
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.jobs.JobReference
import com.dtolabs.rundeck.core.jobs.JobService
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import org.rundeck.app.spi.Services
import org.rundeck.core.executions.provenance.GenericProvenance
import org.rundeck.core.executions.provenance.ProvenanceUtil
import org.rundeck.core.executions.provenance.StepPluginProvenance
import spock.lang.Specification

class StepPluginAdapterSpec extends Specification {
    static class TestPlugin1 implements StepPlugin, DynamicProperties {
        Map<String, Object> projectAndFrameworkValues
        Map<String, Object> dpropsvals
        Services services

        @Override
        void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException {

        }

        @Override
        Map<String, Object> dynamicProperties(final Map<String, Object> projectAndFrameworkValues) {
            this.projectAndFrameworkValues = projectAndFrameworkValues
            return dpropsvals
        }

        @Override
        Map<String, Object> dynamicProperties(
            final Map<String, Object> projectAndFrameworkValues,
            final Services services
        ) {
            this.projectAndFrameworkValues = projectAndFrameworkValues
            this.services = services
            return dpropsvals
        }
    }

    def "dynamicProperties"() {
        given:
            def plugin = new TestPlugin1(dpropsvals: [x: 'y'])
            def sut = new StepPluginAdapter(plugin)
        when:
            def result = sut.dynamicProperties([a: 'b'])
        then:
            result == [x: 'y']
            plugin.projectAndFrameworkValues == [a: 'b']
            plugin.services == null
    }

    def "dynamicProperties with services"() {
        given:
            def plugin = new TestPlugin1(dpropsvals: [x: 'y'])
            def svcs = Mock(Services)
            def sut = new StepPluginAdapter(plugin)
        when:
            def result = sut.dynamicProperties([a: 'b'], svcs)
        then:
            result == [x: 'y']
            plugin.projectAndFrameworkValues == [a: 'b']
            plugin.services == svcs
    }

    static class TestPlugin2 implements StepPlugin, Describable {
        Description testDescription

        @Override
        Description getDescription() {
            return testDescription
        }

        @Override
        void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException {

        }
    }

    def "get description describable"() {
        given:
            Description testDescription = Mock(Description)
            def plugin = new TestPlugin2(testDescription: testDescription)
            def svcs = Mock(Services)
            def sut = new StepPluginAdapter(plugin)
        when:
            def result = sut.description
        then:
            result == testDescription
    }

    @Plugin(service = 'dummy', name = 'testplugin3')
    @PluginDescription(title = 'something')
    static class TestPlugin3 implements StepPlugin {

        @Override
        void executeStep(final PluginStepContext context, final Map<String, Object> configuration)
            throws StepException {

        }
    }

    def "get description annotation"() {
        given:
            Description testDescription = Mock(Description)
            def plugin = new TestPlugin3()
            def sut = new StepPluginAdapter(plugin)
        when:
            def result = sut.description
        then:
            result
            result.name == 'testplugin3'
            result.title == 'something'
    }

    @Plugin(service = ServiceNameConstants.WorkflowStep, name = 'testplugin4')
    @PluginDescription(title = 'something')
    static class TestPlugin4 implements StepPlugin {
        @Delegate
        StepPlugin delegate
    }

    def "execution workflow step"() {
        given:
            def project = 'aproject'
            def mock = Mock(StepPlugin)
            def mockPlugin = new TestPlugin4(delegate: mock)
            def sut = new StepPluginAdapter(mockPlugin)
            def stepContext = Mock(StepExecutionContext) {
                getFrameworkProject() >> project
                getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    getFrameworkProjectMgr() >> Mock(ProjectManager) {
                        getFrameworkProject(project) >> Mock(IRundeckProject)
                    }
                    getPropertyRetriever() >> Mock(PropertyRetriever)
                }
                getStepContext() >> [1, 2]
            }
            def stepItem = Mock(StepExecutionItem) {

            }
        when:
            def result = sut.executeWorkflowStep(stepContext, stepItem)
        then:
            result
            1 * mock.executeStep(_, _) >> {

            }

    }

    def "execution workflow step run job sets provenance no input provenance"() {
        given:
            def project = 'aproject'
            def jobRun = JobService.RunJob.builder().
                jobReference(Mock(JobReference))
                                   .build()
            def mock = Mock(StepPlugin) {
                1 * executeStep(_, _) >> {
                    it[0].executionContext.jobService.runJob(jobRun)
                }
            }
            def mockPlugin = new TestPlugin4(delegate: mock)
            def sut = new StepPluginAdapter(mockPlugin)
            def jobService = Mock(JobService)
            def stepContext = Mock(StepExecutionContext) {
                getFrameworkProject() >> project
                getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    getFrameworkProjectMgr() >> Mock(ProjectManager) {
                        getFrameworkProject(project) >> Mock(IRundeckProject)
                    }
                    getPropertyRetriever() >> Mock(PropertyRetriever)
                }
                getStepContext() >> [1, 2]
                getJobService() >> jobService
                getExecutionListener() >> Mock(ExecutionListener) {
                    log(_, _) >> {
                        System.err.println(it[0] + ": " + it[1])
                    }
                }
            }
            def stepItem = Mock(StepExecutionItem) {

            }
        when:
            def result = sut.executeWorkflowStep(stepContext, stepItem)
        then:
            result
            1 * jobService.runJob(
                { JobService.RunJob req ->
                    req.provenance != null
                    req.provenance.size() == 1
                    req.provenance[0] instanceof StepPluginProvenance
                    ((StepPluginProvenance) req.provenance[0]).data.provider == 'testplugin4'
                    ((StepPluginProvenance) req.provenance[0]).data.service == ServiceNameConstants.WorkflowStep
                    ((StepPluginProvenance) req.provenance[0]).data.stepCtx == '1/2'
                }
            ) >> Mock(ExecutionReference)
    }
    def "execution workflow step run job sets provenance with input provenance"() {
        given:
            def project = 'aproject'
            def jobRun = JobService.RunJob.builder().
                jobReference(Mock(JobReference))
                .provenance([ProvenanceUtil.generic(test:'data')])
                                   .build()
            def mock = Mock(StepPlugin) {
                1 * executeStep(_, _) >> {
                    it[0].executionContext.jobService.runJob(jobRun)
                }
            }
            def mockPlugin = new TestPlugin4(delegate: mock)
            def sut = new StepPluginAdapter(mockPlugin)
            def jobService = Mock(JobService)
            def stepContext = Mock(StepExecutionContext) {
                getFrameworkProject() >> project
                getIFramework() >> Mock(com.dtolabs.rundeck.core.common.IFramework) {
                    getFrameworkProjectMgr() >> Mock(ProjectManager) {
                        getFrameworkProject(project) >> Mock(IRundeckProject)
                    }
                    getPropertyRetriever() >> Mock(PropertyRetriever)
                }
                getStepContext() >> [1, 2]
                getJobService() >> jobService
                getExecutionListener() >> Mock(ExecutionListener) {
                    log(_, _) >> {
                        System.err.println(it[0] + ": " + it[1])
                    }
                }
            }
            def stepItem = Mock(StepExecutionItem) {

            }
        when:
            def result = sut.executeWorkflowStep(stepContext, stepItem)
        then:
            result
            1 * jobService.runJob(
                { JobService.RunJob req ->
                    req.provenance != null
                    req.provenance.size() == 2
                    req.provenance[0] instanceof GenericProvenance
                    ((GenericProvenance) req.provenance[0]).data==[test:'data']
                    req.provenance[1] instanceof StepPluginProvenance
                    ((StepPluginProvenance) req.provenance[1]).data.provider == 'testplugin4'
                    ((StepPluginProvenance) req.provenance[1]).data.service == ServiceNameConstants.WorkflowStep
                    ((StepPluginProvenance) req.provenance[1]).data.stepCtx == '1/2'
                }
            ) >> Mock(ExecutionReference)
    }
}
