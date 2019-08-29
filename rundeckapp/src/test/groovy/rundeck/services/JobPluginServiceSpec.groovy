package rundeck.services

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.PluginControlService
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.JobPluginException
import com.dtolabs.rundeck.core.execution.workflow.StepExecutionContext
import com.dtolabs.rundeck.core.execution.workflow.WorkflowExecutionItem
import com.dtolabs.rundeck.core.jobs.JobExecutionEvent
import com.dtolabs.rundeck.core.jobs.JobEventStatus
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.plugins.jobs.JobExecutionEventImpl
import com.dtolabs.rundeck.plugins.jobs.JobPlugin
import com.dtolabs.rundeck.server.plugins.services.JobPluginProviderService
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import rundeck.CommandExec
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.ScheduledExecutionStats
import rundeck.User
import rundeck.Workflow
import rundeck.services.feature.FeatureService
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(JobPluginService)
@Mock([Execution, ScheduledExecution, Workflow, CommandExec, User, ScheduledExecutionStats])
class JobPluginServiceSpec extends Specification {

    def item = Mock(WorkflowExecutionItem)
    def featureService = Mock(FeatureService){
        featurePresent("job-plugin", false) >> true
    }
    def iRundeckProject = Mock(IRundeckProject){
        hasProperty("project.enable.jobPlugin.TestPlugin") >> true
        getProperty("project.enable.jobPlugin.TestPlugin") >> 'true'
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
    def jobPluginProviderService = Mock(JobPluginProviderService)


    static class JobPluginImpl implements JobPlugin {

        @Override
        public JobEventStatus beforeJobStarts(JobExecutionEvent event)throws JobPluginException{
            throw new JobPluginException("Test job life cycle exception")
        }

        @Override
        public JobEventStatus afterJobEnds(JobExecutionEvent event)throws JobPluginException{
            throw new JobPluginException("Test job life cycle exception")
        }
    }

    @Unroll
    def "custom plugin successful answer via plugin list"() {
        given:
            service.jobPluginProviderService = jobPluginProviderService
            service.frameworkService = frameworkService
            service.featureService = featureService
            def plugin = Mock(JobPluginImpl) {
                beforeJobStarts(_) >> Mock(JobEventStatus) {
                    isSuccessful() >> true
                }
                afterJobEnds(_) >> Mock(JobEventStatus) {
                    isSuccessful() >> true
                }
            }
        when:
            JobEventStatus result = service.handleEvent(
                    eventType == JobPluginService.EventType.BEFORE_RUN ?
                    JobExecutionEventImpl.beforeRun(executionContext, null, null) :
                    JobExecutionEventImpl.afterRun(executionContext, null, null),
                    eventType,
                    [new NamedJobPlugin(name: 'TestPlugin', plugin: plugin)]
            )
        then:
            result.isSuccessful()
        where:
            eventType                             | _
            JobPluginService.EventType.BEFORE_RUN | _
            JobPluginService.EventType.AFTER_RUN  | _
    }

    @Unroll
    def "custom plugin unsuccessful answer via plugin list event #eventType"() {
        given:
            service.jobPluginProviderService = jobPluginProviderService
            service.frameworkService = frameworkService
            service.featureService = featureService
            def plugin = Mock(JobPluginImpl) {
                beforeJobStarts(_) >> Mock(JobEventStatus) {
                    isSuccessful() >> false
                }
                afterJobEnds(_) >> Mock(JobEventStatus) {
                    isSuccessful() >> false
                }
            }
        when:
            JobEventStatus result = service.handleEvent(
                    eventType == JobPluginService.EventType.BEFORE_RUN ?
                    JobExecutionEventImpl.beforeRun(executionContext, null, null) :
                    JobExecutionEventImpl.afterRun(executionContext, null, null),
                    eventType,
                    [new NamedJobPlugin(name: 'TestPlugin', plugin: plugin)]
            )
        then:
            thrown(JobPluginException)
        where:
            eventType                             | _
            JobPluginService.EventType.BEFORE_RUN | _
            JobPluginService.EventType.AFTER_RUN  | _
    }

    @Unroll
    def "custom plugin exception thrown via plugin list event #eventType"() {
        given:
            service.jobPluginProviderService = jobPluginProviderService
            service.frameworkService = frameworkService
            service.featureService = featureService
            def plugin = new JobPluginImpl()
        when:
            JobEventStatus result = service.handleEvent(
                    eventType == JobPluginService.EventType.BEFORE_RUN ?
                    JobExecutionEventImpl.beforeRun(executionContext, null, null) :
                    JobExecutionEventImpl.afterRun(executionContext, null, null),
                    eventType,
                    [new NamedJobPlugin(name: 'TestPlugin', plugin: plugin)]
            )
        then:
            thrown(JobPluginException)
        where:
            eventType                             | _
            JobPluginService.EventType.BEFORE_RUN | _
            JobPluginService.EventType.AFTER_RUN  | _
    }

    def "create configured plugins with no project defaults"() {
        given:
            def configs = PluginConfigSet.with(
                    'JobPlugin', [
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
            def configured1 = new ConfiguredPlugin<JobPlugin>(null, [a: 'b'])
        when:
            def result = service.createConfiguredPlugins(configs, project)
        then:
            result.size() == 1
            result[0].name == 'typeA'

            1 * service.pluginService.configurePlugin('typeA', [a: 'b'], 'aProject', _, JobPlugin) >> configured1

    }

    def "list enabled plugins"() {
        given:
            service.featureService = featureService
            def controlService = Mock(PluginControlService) {
                isDisabledPlugin('typeA', 'JobPlugin') >> true
            }
            service.pluginService = Mock(PluginService) {
                listPlugins(JobPlugin) >> [
                        typeA: new DescribedPlugin<JobPlugin>(null, null, 'typeA'),
                        typeB: new DescribedPlugin<JobPlugin>(null, null, 'typeB')
                ]
            }

        when:
            def result = service.listEnabledJobPlugins(controlService)
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
            def status = Mock(JobEventStatus) {
                useNewValues() >> useNewValues
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

}
