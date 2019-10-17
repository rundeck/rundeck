package rundeck.services

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.plugins.jobs.JobOptionImpl
import grails.test.mixin.TestFor
import spock.lang.Specification

@TestFor(JobLifecyclePluginService)
class JobLifecyclePluginServiceSpec extends Specification {

    class JobLifecycleStatusImplTest implements JobLifecycleStatus{

        boolean useNewValues = true
        Map newOptionValues = ["firstKey":"newModifiedValue", "secondKey":"secondValue"]
        @Override
        SortedSet<JobOption> getOptions() {
            SortedSet<JobOption> jobOptions = new TreeSet<JobOption>()
            jobOptions.add(new JobOptionImpl([
                    name: "newTestOption"
            ]))
            jobOptions.add(new JobOptionImpl([
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
                    (JobLifecyclePluginService.CONF_PROJECT_ENABLED + 'typeA'): 'true',
                    (JobLifecyclePluginService.CONF_PROJECT_ENABLED + 'typeB'): 'true',
                    (JobLifecyclePluginService.CONF_PROJECT_ENABLED + 'typeC'): 'false',
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
        initial.add(new JobOptionImpl([name: "oldTestOption"]))
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
        initial.add(new JobOptionImpl([name: "oldTestOption"]))
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

}
