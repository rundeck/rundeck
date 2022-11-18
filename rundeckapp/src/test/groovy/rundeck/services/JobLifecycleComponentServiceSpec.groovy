package rundeck.services

import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.jobs.JobLifecycleComponent
import com.dtolabs.rundeck.core.jobs.JobLifecycleComponentException
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatus
import com.dtolabs.rundeck.core.jobs.JobLifecycleStatusImpl
import com.dtolabs.rundeck.core.jobs.JobOption
import com.dtolabs.rundeck.core.jobs.JobPersistEvent
import com.dtolabs.rundeck.core.jobs.JobPreExecutionEvent
import com.dtolabs.rundeck.core.plugins.ConfiguredPlugin
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
                    name: 'test', component: Mock(JobLifecycleComponent) {
                beforeSaveJob(_) >> {
                    throw new JobLifecycleComponentException("oops")
                }
            }
            )]
        when:
            def result = service.handleEvent(evt, type, plugins)
        then:
            JobLifecycleComponentException err = thrown()
            err.message =~ /oops/
        where:
            type                                            | _
            JobLifecycleComponentService.EventType.BEFORE_SAVE | _
    }
    
    def "handleEvent plugin exception PRE_EXECUTION"() {
        given:
            def evt = Mock(JobPreExecutionEvent)
            def plugins = [new NamedJobLifecycleComponent(
                    name: 'test', component: Mock(JobLifecycleComponent) {
                beforeJobExecution(_) >> {
                    throw new JobLifecycleComponentException("oops")
                }
            }
            )]
        when:
            def result = service.handleEvent(evt, type, plugins)
        then:
        JobLifecycleComponentException err = thrown()
            err.message =~ /oops/
        where:
            type                                            | _
            JobLifecycleComponentService.EventType.PRE_EXECUTION | _
    }


    def "merge execution metadata replacing values"() {
        given:

        def status = JobLifecycleStatusImpl.builder()
            .useNewMetadata(newMeta != null && !newMeta.isEmpty())
            .newExecutionMetadata(newMeta)
            .build()


        when:
        def result = service.mergePreExecutionMetadata(initial, status)

        then:
        (result == null) == (initial == null && !status.useNewMetadata)
        result?.size() == resultSize
        result?.first == firstResult
        result?.second == secondResult
        result?.third == thirdResult
        result?.fourth == fourthResult

        where:
        initial                                                  | newMeta                        | resultSize | firstResult | secondResult | thirdResult | fourthResult
        null                                                     | null                           | null       | null        | null         | null        | null
        ["first": "first", "second": "second", "third": "third"] | null                           | 3          | "first"     | "second"     | "third"     | null
        ["first": "first", "second": "second", "third": "third"] | ["second": "2", "fourth": "4"] | 4          | "first"     | "2"          | "third"     | "4"
        null                                                     | ["second": "2", "fourth": "4"] | 2          | null        | "2"          | null        | "4"


    }

    def "lifecycle component loading"() {
        given:
        def project = Stub(IRundeckProject) {
            getProjectProperties() >> {
                Map<String,String> map = new LinkedHashMap()
                map.put(JobLifecycleComponentService.CONF_PROJECT_ENABLED + 'pluginA', 'true')
                map.put(JobLifecycleComponentService.CONF_PROJECT_ENABLED + 'pluginB', 'true')
                return map
            }
        }

        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject("TestProject") >> {
                return project
            }
        }

        service.pluginService = Stub(PluginService) {
            configurePlugin(*_) >> { args ->
                return new ConfiguredPlugin<JobLifecyclePlugin>(new JLPluginTestImpl(name: args[0]), [:])
            }
        }

        service.beanComponents = [
            new JLCompTestImpl(name: "Comp1"),
            new JLCompTestImpl(name: "Comp2")
        ]

        when:
        def result = service.loadProjectComponents("TestProject")

        then:
        result.size() == 4
        result[0].component instanceof JLCompTestImpl
        result[0].component.name == "Comp1"
        result[1].component instanceof JLCompTestImpl
        result[1].component.name == "Comp2"
        result[2].component instanceof JLPluginTestImpl
        result[2].component.name == "pluginA"
        result[3].component instanceof JLPluginTestImpl
        result[3].component.name == "pluginB"

    }


    def "lifecycle component loading with plugins only"() {
        given:
        def project = Stub(IRundeckProject) {
            getProjectProperties() >> {
                Map<String,String> map = new LinkedHashMap()
                map.put(JobLifecycleComponentService.CONF_PROJECT_ENABLED + 'pluginA', 'true')
                map.put(JobLifecycleComponentService.CONF_PROJECT_ENABLED + 'pluginB', 'true')
                return map
            }
        }

        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject("TestProject") >> {
                return project
            }
        }

        service.pluginService = Stub(PluginService) {
            configurePlugin(*_) >> { args ->
                return new ConfiguredPlugin<JobLifecyclePlugin>(new JLPluginTestImpl(name: args[0]), [:])
            }
        }

        service.beanComponents = null

        when:
        def result = service.loadProjectComponents("TestProject")

        then:
        result.size() == 2
        result[0].component instanceof JLPluginTestImpl
        result[0].component.name == "pluginA"
        result[1].component instanceof JLPluginTestImpl
        result[1].component.name == "pluginB"

    }


    def "lifecycle component loading with internal components only"() {
        given:
        def project = Stub(IRundeckProject) {
            getProjectProperties() >> [:]
        }

        service.frameworkService = Mock(FrameworkService) {
            getFrameworkProject("TestProject") >> {
                return project
            }
        }

        service.beanComponents = [
            new JLCompTestImpl(name: "Comp1"),
            new JLCompTestImpl(name: "Comp2")
        ]

        when:
        def result = service.loadProjectComponents("TestProject")

        then:
        result.size() == 2
        result[0].component instanceof JLCompTestImpl
        result[0].component.name == "Comp1"
        result[1].component instanceof JLCompTestImpl
        result[1].component.name == "Comp2"

    }


    class JLCompTestImpl implements JobLifecycleComponent {

        String name

        @Override
        JobLifecycleStatus beforeJobExecution(JobPreExecutionEvent event) throws JobLifecycleComponentException {
            return null
        }

        @Override
        JobLifecycleStatus beforeSaveJob(JobPersistEvent event) throws JobLifecycleComponentException {
            return null
        }
    }

    class JLPluginTestImpl implements JobLifecyclePlugin {
        String name

    }

}
