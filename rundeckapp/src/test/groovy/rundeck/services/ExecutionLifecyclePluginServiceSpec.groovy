package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.ExecutionLifecyclePluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent
import com.dtolabs.rundeck.core.jobs.ExecutionLifecycleStatus
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl
import com.dtolabs.rundeck.server.plugins.services.ExecutionLifecyclePluginProviderService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import rundeck.services.feature.FeatureService
import rundeck.services.feature.FeatureServiceSpec
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(ExecutionLifecyclePluginService)
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, User, ScheduledExecutionStats])
class ExecutionLifecyclePluginServiceSpec extends Specification {

    def item = Mock(WorkflowExecutionItem)
    def featureService = Mock(FeatureService){
        featurePresent("executionLifecyclePlugin", false) >> true
    }
    def iRundeckProject = Mock(IRundeckProject){
        hasProperty("project.enable.executionLifecyclePlugin.TestPlugin") >> true
        getProperty("project.enable.executionLifecyclePlugin.TestPlugin") >> 'true'
    }
    def frameworkService = Mock(FrameworkService){
        getFrameworkProject("Test") >> iRundeckProject
    }
    def projectManager = Mock(ProjectManager) {
        getFrameworkProject("Test") >> iRundeckProject
    }
    def framework = Mock(Framework){
        getProjectManager() >> projectManager

    }
    StepExecutionContext executionContext = Mock(StepExecutionContext) {
        getFrameworkProject() >> "Test"
        getFramework() >> framework
    }
    def executionLifecyclePluginProviderService = Mock(ExecutionLifecyclePluginProviderService)


    static class ExecutionLifecyclePluginImpl implements ExecutionLifecyclePlugin {

        @Override
        public ExecutionLifecycleStatus beforeJobStarts(JobExecutionEvent event)throws ExecutionLifecyclePluginException{
            throw new ExecutionLifecyclePluginException("Test job life cycle exception")
        }

        @Override
        public ExecutionLifecycleStatus afterJobEnds(JobExecutionEvent event)throws ExecutionLifecyclePluginException{
            throw new ExecutionLifecyclePluginException("Test job life cycle exception")
        }
    }

    @Unroll
    def "custom plugin successful answer via plugin list"() {
        given:
            service.executionLifecyclePluginProviderService = executionLifecyclePluginProviderService
            service.frameworkService = frameworkService
            service.featureService = featureService
            def plugin = Mock(ExecutionLifecyclePluginImpl) {
                beforeJobStarts(_) >> Mock(ExecutionLifecycleStatus) {
                    isSuccessful() >> true
                }
                afterJobEnds(_) >> Mock(ExecutionLifecycleStatus) {
                    isSuccessful() >> true
                }
            }
        when:
            ExecutionLifecycleStatus result = service.handleEvent(
                    eventType == ExecutionLifecyclePluginService.EventType.BEFORE_RUN ?
                    JobExecutionEventImpl.beforeRun(executionContext, null, null) :
                    JobExecutionEventImpl.afterRun(executionContext, null, null),
                    eventType,
                    [new NamedExecutionLifecyclePlugin(name: 'TestPlugin', plugin: plugin)]
            )
        then:
            result.isSuccessful()
        where:
            eventType                                            | _
            ExecutionLifecyclePluginService.EventType.BEFORE_RUN | _
            ExecutionLifecyclePluginService.EventType.AFTER_RUN  | _
    }

    @Unroll
    def "custom plugin unsuccessful answer via plugin list event #eventType"() {
        given:
            service.executionLifecyclePluginProviderService = executionLifecyclePluginProviderService
            service.frameworkService = frameworkService
            service.featureService = featureService
            def plugin = Mock(ExecutionLifecyclePluginImpl) {
                beforeJobStarts(_) >> Mock(ExecutionLifecycleStatus) {
                    isSuccessful() >> false
                }
                afterJobEnds(_) >> Mock(ExecutionLifecycleStatus) {
                    isSuccessful() >> false
                }
            }
        when:
            ExecutionLifecycleStatus result = service.handleEvent(
                    eventType == ExecutionLifecyclePluginService.EventType.BEFORE_RUN ?
                    JobExecutionEventImpl.beforeRun(executionContext, null, null) :
                    JobExecutionEventImpl.afterRun(executionContext, null, null),
                    eventType,
                    [new NamedExecutionLifecyclePlugin(name: 'TestPlugin', plugin: plugin)]
            )
        then:
            thrown(ExecutionLifecyclePluginException)
        where:
            eventType                                            | _
            ExecutionLifecyclePluginService.EventType.BEFORE_RUN | _
            ExecutionLifecyclePluginService.EventType.AFTER_RUN  | _
    }

    @Unroll
    def "custom plugin exception thrown via plugin list event #eventType"() {
        given:
            service.executionLifecyclePluginProviderService = executionLifecyclePluginProviderService
            service.frameworkService = frameworkService
            service.featureService = featureService
            def plugin = new ExecutionLifecyclePluginImpl()
        when:
            ExecutionLifecycleStatus result = service.handleEvent(
                    eventType == ExecutionLifecyclePluginService.EventType.BEFORE_RUN ?
                    JobExecutionEventImpl.beforeRun(executionContext, null, null) :
                    JobExecutionEventImpl.afterRun(executionContext, null, null),
                    eventType,
                    [new NamedExecutionLifecyclePlugin(name: 'TestPlugin', plugin: plugin)]
            )
        then:
            thrown(ExecutionLifecyclePluginException)
        where:
            eventType                                            | _
            ExecutionLifecyclePluginService.EventType.BEFORE_RUN | _
            ExecutionLifecyclePluginService.EventType.AFTER_RUN  | _
    }

    def "create configured plugins with no project defaults"() {
        given:
            def configs = PluginConfigSet.with(
                    ServiceNameConstants.ExecutionLifecycle, [
                    SimplePluginConfiguration.builder().provider('typeA').configuration([a: 'b']).build()
            ]
            )
            String project = 'aProject'
            service.pluginService = Mock(PluginService)
            service.frameworkService = Mock(FrameworkService) {
                getFrameworkProject(project) >> Mock(IRundeckProject) {
                    getProjectProperties() >> [
                            :
                    ]
                }
            }
            def configured1 = new ConfiguredPlugin<ExecutionLifecyclePluginImpl>(null, [a: 'b'])
        when:
            def result = service.createConfiguredPlugins(configs, project)
        then:
            result.size() == 1
            result[0].name == 'typeA'

            1 * service.pluginService.configurePlugin('typeA', [a: 'b'], 'aProject', _, ExecutionLifecyclePlugin) >> configured1

    }

    def "list enabled plugins"() {
        given:
            service.featureService = featureService
            def controlService = Mock(PluginControlService) {
                isDisabledPlugin('typeA', ServiceNameConstants.ExecutionLifecycle) >> true
            }
            service.pluginService = Mock(PluginService) {
                listPlugins(ExecutionLifecyclePlugin) >> [
                        typeA: new DescribedPlugin<ExecutionLifecyclePluginImpl>(null, null, 'typeA'),
                        typeB: new DescribedPlugin<ExecutionLifecyclePluginImpl>(null, null, 'typeB')
                ]
            }

        when:
            def result = service.listEnabledExecutionLifecyclePlugins(controlService)
        then:
            result.size() == 1
            result['typeA'] == null
            result['typeB'] != null
    }

    def "merge job before run execution event"() {
        given:
            def ctx1 = Mock(StepExecutionContext) {
                getFrameworkProject() >> 'ATest'
                getLoglevel() >> 2
            }
            def ref = Mock(ExecutionReference)
            def wf = Mock(WorkflowExecutionItem)
            def event = JobExecutionEventImpl.beforeRun(ctx1, ref, wf)
            def status = Mock(ExecutionLifecycleStatus) {
                isUseNewValues() >> useNewValues
                getExecutionContext() >> Mock(StepExecutionContext) {
                    getLoglevel() >> 0
                }
            }
        when:
            JobExecutionEvent result = (JobExecutionEvent) service.mergeEvent(status, event)
        then:
            result != null
            result.executionContext
            result.executionContext.frameworkProject == 'ATest'
            result.execution == ref
            result.workflow == wf

        where:
            useNewValues | expectlevel
            true         | 0
            false        | 2
    }

    def "set exec lifecycle plugin config"() {
        given:
            ScheduledExecution job = ScheduledExecution.
                    fromMap([jobName: 'test', project: 'aProject', sequence: [commands: [[exec: 'echo test']]]])
            def configSet =
                    PluginConfigSet.with ServiceNameConstants.ExecutionLifecycle, [
                            SimplePluginConfiguration.builder().provider('aProvider').configuration([a: 'b']).build()
                    ]
        when:
            service.setExecutionLifecyclePluginConfigSetForJob(job, configSet)
        then:
            job.pluginConfigMap != null
            job.pluginConfigMap['ExecutionLifecycle'] != null
            job.pluginConfigMap['ExecutionLifecycle'] == [aProvider: [a: 'b']]
    }

    def "set exec lifecycle multi plugin config"() {
        given:
            ScheduledExecution job = ScheduledExecution.
                    fromMap([jobName: 'test', project: 'aProject', sequence: [commands: [[exec: 'echo test']]]])
            def configSet =
                    PluginConfigSet.with ServiceNameConstants.ExecutionLifecycle, [
                            SimplePluginConfiguration.builder().provider('aProvider').configuration([a: 'b']).build(),
                            SimplePluginConfiguration.builder().provider('bProvider').configuration([b: 'c']).build(),
                    ]
        when:
            service.setExecutionLifecyclePluginConfigSetForJob(job, configSet)
        then:
            job.pluginConfigMap != null
            job.pluginConfigMap['ExecutionLifecycle'] != null
            job.pluginConfigMap['ExecutionLifecycle'] == [aProvider: [a: 'b'], bProvider: [b: 'c']]
    }
    def "set exec lifecycle multi plugin config null"() {
        given:
            ScheduledExecution job = ScheduledExecution.
                    fromMap([jobName: 'test', project: 'aProject', sequence: [commands: [[exec: 'echo test']]]])
            def configSet =null
        when:
            service.setExecutionLifecyclePluginConfigSetForJob(job, configSet)
        then:
            job.pluginConfigMap != null
            job.pluginConfigMap['ExecutionLifecycle'] == null
    }

    def "get exec lifecycle multi plugin config"() {
        given:
            ScheduledExecution job = ScheduledExecution.
                    fromMap([jobName: 'test', project: 'aProject', sequence: [commands: [[exec: 'echo test']]]])
            job.pluginConfigMap = [ExecutionLifecycle: [aProvider: [a: 'b'], bProvider: [b: 'c']]]

            service.featureService = Mock(FeatureService) {
                featurePresent('executionLifecyclePlugin', false) >> true
            }
        when:
            def result = service.getExecutionLifecyclePluginConfigSetForJob(job)
        then:
            result.service == ServiceNameConstants.ExecutionLifecycle
            result.pluginProviderConfigs
            result.pluginProviderConfigs.size() == 2
            result.pluginProviderConfigs[0].provider == 'aProvider'
            result.pluginProviderConfigs[0].configuration == [a: 'b']
            result.pluginProviderConfigs[1].provider == 'bProvider'
            result.pluginProviderConfigs[1].configuration == [b: 'c']
    }
}
