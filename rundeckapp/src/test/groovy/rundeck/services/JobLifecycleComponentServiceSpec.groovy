package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.JobPersistEvent
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent
import com.dtolabs.rundeck.core.plugins.JobLifecyclePluginException
import com.dtolabs.rundeck.plugins.jobs.JobOptionImpl
import com.dtolabs.rundeck.plugins.project.JobLifecyclePlugin
import grails.testing.services.ServiceUnitTest
import spock.lang.Specification


class JobLifecycleComponentServiceSpec extends Specification implements ServiceUnitTest<JobLifecycleComponentService> {

    class JobLifecycleStatusImplTest implements JobLifecycleStatus{

        boolean useNewValues = true
        Map newOptionValues = ["firstKey":"newModifiedValue", "secondKey":"secondValue"]
        @Override
        SortedSet<JobOption> getOptions() {
            SortedSet<JobOption> jobOptions = new TreeSet<JobOption>()
            jobOptions.add(JobOptionImpl.fromOptionMap([
                    name: "newTestOption"
            ]))
            jobOptions.add(JobOptionImpl.fromOptionMap([
                    name: "newTestOption2"
            ]))
            return jobOptions
        }

        @Override
        Map getOptionsValues() {
            return newOptionValues
        }

    }

    def "get project default job plugin types"() {
        given:
        def project = Mock(IRundeckProject) {
            getProjectProperties() >> [
                    (JobLifecycleComponentService.CONF_PROJECT_ENABLED + 'typeA'): 'true',
                    (JobLifecycleComponentService.CONF_PROJECT_ENABLED + 'typeB'): 'true',
                    (JobLifecycleComponentService.CONF_PROJECT_ENABLED + 'typeC'): 'false',
            ]
        }
        when:
        def result = service.getProjectDefaultJobLifecyclePlugins(project)
        then:
        result == (['typeA', 'typeB'] as Set)

    }

    def "merge options with new values"(){
        given:
        SortedSet<JobOption> initial = new TreeSet<JobOption>()
        initial.add(JobOptionImpl.fromOptionMap([name: "oldTestOption"]))
        JobLifecycleStatus jobEventStatus = new JobLifecycleStatusImplTest()

        when:
        def result = service.mergePersistOptions(initial, jobEventStatus)

        then:
        result
        result.size() == 2
        result[0].name == "newTestOption"
        result[1].name == "newTestOption2"

    }

    def "merge options with new values, with no replace"(){
        given:
        SortedSet<JobOption> initial = new TreeSet<JobOption>()
        initial.add(JobOptionImpl.fromOptionMap([name: "oldTestOption"]))
        JobLifecycleStatus jobEventStatus = new JobLifecycleStatusImplTest()
        jobEventStatus.useNewValues = false

        when:
        def result = service.mergePersistOptions(initial, jobEventStatus)

        then:
        result
        result.size() == 1
        result[0].name == "oldTestOption"

    }

    def "merge option values, modifing value" (){
        given:
        Map<String, String> optionsValues = ["firstKey":"firstValue"]
        JobLifecycleStatus jobEventStatus = new JobLifecycleStatusImplTest()

        when:
        def result = service.mergePreExecutionOptionsValues(optionsValues, jobEventStatus)

        then:
        result
        result.size() == 2
        result.firstKey == "newModifiedValue"
    }

    def "merge option values, adding value" (){
        given:
        Map<String, String> optionsValues = ["firstKey":"firstValue"]
        JobLifecycleStatus jobEventStatus = new JobLifecycleStatusImplTest()
        jobEventStatus.newOptionValues << ["thirdKey":"new third value"]

        when:
        def result = service.mergePreExecutionOptionsValues(optionsValues, jobEventStatus)

        then:
        result
        result.size() == 3
        result.thirdKey == "new third value"
    }

    def "merge option values, without new values" (){
        given:
        Map<String, String> optionsValues = ["firstKey":"firstValue"]
        JobLifecycleStatus jobEventStatus = new JobLifecycleStatusImplTest()
        jobEventStatus.newOptionValues << ["thirdKey":"new third value"]
        jobEventStatus.useNewValues = false

        when:
        def result = service.mergePreExecutionOptionsValues(optionsValues, jobEventStatus)

        then:
        result
        result.size() == 1
        !result.thirdKey
        result.firstKey == "firstValue"
    }

    def "handleEvent no plugins PRE_EXECUTION"() {
        given:
            def evt = Mock(JobPreExecutionEvent)
            def plugins = []
        when:
            def result = service.handleEvent(evt, type, plugins)
        then:
            result==null
        where:
            type                                              | _
            JobLifecycleComponentService.EventType.PRE_EXECUTION | _
    }

    def "handleEvent no plugins BEFORE_SAVE"() {
        given:
            def evt = Mock(JobPersistEvent)
            def plugins = []
        when:
            def result = service.handleEvent(evt, type, plugins)
        then:
            result==null
        where:
            type                                            | _
            JobLifecycleComponentService.EventType.BEFORE_SAVE | _
    }

    def "handleEvent plugin exception BEFORE_SAVE"() {
        given:
            def evt = Mock(JobPersistEvent)
            def plugins = [new NamedJobLifecycleComponent(
                    name: 'test', plugin: Mock(JobLifecyclePlugin) {
                beforeSaveJob(_) >> {
                    throw new JobLifecyclePluginException("oops")
                }
            }
            )]
        when:
            def result = service.handleEvent(evt, type, plugins)
        then:
            JobLifecyclePluginException err = thrown()
            err.message =~ /oops/
        where:
            type                                            | _
            JobLifecycleComponentService.EventType.BEFORE_SAVE | _
    }
    def "handleEvent plugin exception PRE_EXECUTION"() {
        given:
            def evt = Mock(JobPreExecutionEvent)
            def plugins = [new NamedJobLifecycleComponent(
                    name: 'test', plugin: Mock(JobLifecyclePlugin) {
                beforeJobExecution(_) >> {
                    throw new JobLifecyclePluginException("oops")
                }
            }
            )]
        when:
            def result = service.handleEvent(evt, type, plugins)
        then:
            JobLifecyclePluginException err = thrown()
            err.message =~ /oops/
        where:
            type                                            | _
            JobLifecycleComponentService.EventType.PRE_EXECUTION | _
    }
}
