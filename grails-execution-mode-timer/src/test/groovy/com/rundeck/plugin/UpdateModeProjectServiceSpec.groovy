package com.rundeck.plugin

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.fasterxml.jackson.databind.ObjectMapper
import grails.testing.services.ServiceUnitTest
import org.quartz.Scheduler
import org.quartz.Trigger
import spock.lang.Specification
import spock.lang.Unroll

import java.text.DateFormat
import java.text.SimpleDateFormat

class UpdateModeProjectServiceSpec extends Specification implements ServiceUnitTest<UpdateModeProjectService>{

    def setup() {
    }

    def cleanup() {
    }


    def "test simple saveExecutionLaterSettings without changes"(){
        given:
        String project = "TestProject"

        def propertiesData = ["project.disable.schedule": "false",
                              "project.disable.executions": "false",
                              "project.later.executions.enable": "false",
                              "project.later.executions.disable": "true",
                              "project.later.executions.enable.value": null,
                              "project.later.executions.disable.value": "1h",
                              "project.later.schedule.enable": "false",
                              "project.later.schedule.disable": "true",
                              "project.later.schedule.enable.value": null,
                              "project.later.schedule.disable.value": "1h",
        ]

        Properties properties = new Properties()
        propertiesData.each {key, value->
            if(value){
                properties.put(key,value)
            }
        }

        String propertiesString = '{"executions":{"active":"false", "action":"disable", "value":"1h"}, "schedule":{"active":"false", "action":"disable", "value":"1h"}}'

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
        }

        def mockFrameworkService = new MockFrameworkService(
                frameworkProjectsTestData: [
                        TestProject: [projectProperties: propertiesData]
                ]
        )
        mockFrameworkService.setRundeckProject(rundeckProject)


        service.frameworkService  = mockFrameworkService

        when:
        def result = service.saveExecutionLaterSettings(project,properties)
        then:
        result==false


    }


    def "test simple saveExecutionLaterSettings new settigns"(){
        given:
        String project = "TestProject"

        def propertiesData = ["project.disable.schedule": "false",
                              "project.disable.executions": "false",
                              "project.later.executions.enable": "false",
                              "project.later.executions.disable": "true",
                              "project.later.executions.enable.value": null,
                              "project.later.executions.disable.value": "1h",
                              "project.later.schedule.enable": "false",
                              "project.later.schedule.disable": "true",
                              "project.later.schedule.enable.value": null,
                              "project.later.schedule.disable.value": "1h",
        ]

        Properties properties = new Properties()
        propertiesData.each {key, value->
            if(value){
                properties.put(key,value)
            }
        }

        String propertiesString = "bad-data"

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                if(propertiesString){
                    args[1].write(propertiesString?.bytes)
                    propertiesString?.length()
                }
            }
        }

        def mockFrameworkService = new MockFrameworkService(
                frameworkProjectsTestData: [
                        TestProject: [projectProperties: propertiesData]
                ]
        )
        mockFrameworkService.setRundeckProject(rundeckProject)


        service.frameworkService  = mockFrameworkService
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            1*scheduleJob(_,_)
        }
        when:
        def result = service.saveExecutionLaterSettings(project,properties)
        then:
        result==true


    }

    def "test simple saveExecutionLaterSettings with changes"(){
        given:
        String project = "TestProject"

        def propertiesData = ["project.disable.schedule": "false",
                              "project.disable.executions": "false",
                              "project.later.executions.enable": "false",
                              "project.later.executions.disable": executionLater,
                              "project.later.executions.enable.value": null,
                              "project.later.executions.disable.value": "3h",
                              "project.later.schedule.enable": "false",
                              "project.later.schedule.disable": schedulerLater,
                              "project.later.schedule.enable.value": null,
                              "project.later.schedule.disable.value": "3h",
        ]

        Properties properties = new Properties()
        propertiesData.each {key, value->
            if(value){
                properties.put(key,value)
            }
        }

        String propertiesString = '{"executions":{"active":"false", "action":"disable", "value":"1h"}, "schedule":{"active":"false", "action":"disable", "value":"1h"}}'

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
        }

        def mockFrameworkService = new MockFrameworkService(
                frameworkProjectsTestData: [
                        TestProject: [projectProperties: propertiesData]
                ]
        )
        mockFrameworkService.setRundeckProject(rundeckProject)


        service.frameworkService  = mockFrameworkService
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            quartzCalls*scheduleJob(_,_)
        }
        when:
        def result = service.saveExecutionLaterSettings(project,properties)
        then:
        result==resultStatus

        where:
        executionLater | schedulerLater | quartzCalls | resultStatus
        "false"       | "false"        |      0 | false
        "true"        | "false"        |      1 | true
        "true"        | "true"         |      1 | true


    }

    def "test saveExecutionLaterSettings enable or disable now has changed"(){
        given:
        String project = "TestProject"

        def propertiesData = ["project.disable.schedule": scheduleDisable,
                              "project.disable.executions": executionDisable,
                              "project.later.executions.enable": "false",
                              "project.later.executions.disable": "true",
                              "project.later.executions.enable.value": null,
                              "project.later.executions.disable.value": "1h",
                              "project.later.schedule.enable": "false",
                              "project.later.schedule.disable": "true",
                              "project.later.schedule.enable.value": null,
                              "project.later.schedule.disable.value": "1h",
        ]

        Properties properties = new Properties()
        propertiesData.each {key, value->
            if(value){
                properties.put(key,value)
            }
        }

        String propertiesString = '{"global": {"executionDisable":false,"scheduleDisable":false},"executions":{"active":false, "action":"disable", "value":"1h"}, "schedule":{"active":false, "action":"disable", "value":"1h"}}'

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
        }

        def mockFrameworkService = new MockFrameworkService(
                frameworkProjectsTestData: [
                        TestProject: [projectProperties: propertiesData]
                ]
        )
        mockFrameworkService.setRundeckProject(rundeckProject)

        service.frameworkService  = mockFrameworkService
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            quartzCalls*getTrigger(_) >> Mock(org.quartz.Trigger)
            quartzCalls*deleteJob(_)
        }
        when:
        def result = service.saveExecutionLaterSettings(project,properties)
        then:
        result==resultStatus

        where:
        executionDisable | scheduleDisable | quartzCalls | resultStatus
        "false"          | "false"         |     0 | false
        "true"           | "false"         |     1 | true
        "true"           | "true"          |     2 | true
    }

    def "should create one quartz job when schedule and execution changes are at same time"(){
        given:
        String project = "TestProject"

        def propertiesData = ["project.disable.schedule": initialScheduleDisable.toString(),
                              "project.disable.executions": initialExecutionDisable.toString(),
                              "project.later.executions.enable": (!disableExecLat).toString(),
                              "project.later.executions.disable": disableExecLat.toString(),
                              "project.later.executions.enable.value": execTime,
                              "project.later.executions.disable.value": execTime,
                              "project.later.schedule.enable": enableSchedLat.toString(),
                              "project.later.schedule.disable": (!enableSchedLat).toString(),
                              "project.later.schedule.enable.value": schedTime,
                              "project.later.schedule.disable.value": schedTime,
        ]

        Properties properties = new Properties()
        propertiesData.each {key, value->
            if(value){
                properties.put(key,value)
            }
        }

        String propertiesString = "{\"global\": {\"executionDisable\":"+initialExecutionDisable.toString()+",\"scheduleDisable\":"+initialScheduleDisable.toString()+"},\"executions\":{\"active\":false, \"action\":\"null\", \"value\":\"null\"}, \"schedule\":{\"active\":false, \"action\":\"null\", \"value\":\"null\"}}"

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
        }

        def mockFrameworkService = new MockFrameworkService(
                frameworkProjectsTestData: [
                        TestProject: [projectProperties: propertiesData]
                ]
        )
        mockFrameworkService.setRundeckProject(rundeckProject)

        service.frameworkService  = mockFrameworkService
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            0*getTrigger(_) >> Mock(org.quartz.Trigger)
            expectedQuartzCalls*scheduleJob(_,_) >> new Date(1355270400000)
            0*deleteJob(_)
        }
        when:
        def result = service.saveExecutionLaterSettings(project,properties)
        then:
        result==expectedResultStatus

        where:
        initialExecutionDisable | initialScheduleDisable | disableExecLat | execTime | enableSchedLat| schedTime | expectedQuartzCalls | expectedResultStatus
        false                   | true                   |       true     |    "1m"  |       true    | "1m"      | 1                    | true
        false                   | true                   |       true     |    "2m"  |       true    | "1m"      | 2                    | true
        true                    | false                  |       false    |    "1m"  |       false   | "1m"      | 1                    | true
        true                    | false                  |       false    |    "1h"  |       false   | "1m"      | 2                    | true
    }

    def "test editProject"() {
        given:
        String project = "TestProject"
        String propertiesString = '{}'

        def propertiesData = ["project.disable.schedule": disableSchedule,
                              "project.disable.executions": disableExecution,
                              "project.later.executions.disable": "true",
                              "project.later.executions.disable.value": "3h",
                              "project.later.schedule.disable": "true",
                              "project.later.schedule.disable.value": "3h"
        ]

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
            getProjectProperties() >> propertiesData
        }

        def mockFrameworkService = new MockFrameworkService()
        mockFrameworkService.setRundeckProject(rundeckProject)
        mockFrameworkService.notifyProjectSchedulingChange(project,executionLater,scheduleLater,true)

        def quartzScheduler = Mock(Scheduler){
            schedule * scheduleJob(_)
            delete * deleteJob(_)
        }

        service.frameworkService  = mockFrameworkService
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false,scheduler: quartzScheduler)

        when:
        def result = service.editProject(rundeckProject,project,disable, executionLater, scheduleLater)
        then:
        result!=null
        _ * service.frameworkService.notifyProjectSchedulingChange(_,_,_,_)

        where:
        disableExecution  | disableSchedule  | disable | executionLater | scheduleLater| delete | schedule
        "false"           | "false"          | true    | true           | false        | 1      | 0
        "false"           | "false"          | true    | false          | true         | 1      | 0
        "true"            | "false"          | false   | true           | false        | 0      | 1
        "false"           | "true"           | false   | false          | true         | 0      | 1

    }

    def "test initProcess"() {
        given:

        def propertiesData = ["project.disable.schedule": "false",
                              "project.disable.executions": "false",
                              "project.later.executions.disable": "true",
                              "project.later.executions.disable.value": "3h",
                              "project.later.schedule.disable": "true",
                              "project.later.schedule.disable.value": "3h"
        ]
        def projectList=['TestProject']


        DateFormat dateFormat = new SimpleDateFormat(UpdateModeProjectService.DATE_FORMAT);
        Date date = new Date()
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.MINUTE, savedTime)

        def savedDate = dateFormat.format(calendar.getTime())

        String propertiesString = '{"executions":{"active":"true", "action":"disable", "value":"'+executionValue+'","dateSaved":"'+savedDate+'"}, "schedule":{"active":"true", "action":"disable", "value":"'+scheduleValue+'","dateSaved":"'+savedDate+'"}}'

        def rundeckProject = Mock(IRundeckProject){
            existsFileResource(_) >> true
            loadFileResource(_, _) >> { args ->
                args[1].write(propertiesString.bytes)
                propertiesString.length()
            }
            getProjectProperties() >> propertiesData
        }

        def mockFrameworkService = new MockFrameworkService( projectList: projectList, frameworkProjectsTestData: [
                TestProject: [projectProperties: propertiesData]
        ])
        mockFrameworkService.setRundeckProject(rundeckProject)

        service.frameworkService = mockFrameworkService
        service.quartzScheduler = Mock(Scheduler)
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        when:
        service.initProcess()

        then:
        scheduleCalls* service.quartzScheduler.scheduleJob(_,_)

        where:
        executionValue | scheduleValue | savedTime | scheduleCalls
        "2h"           | "2h"          | -110      | 2
        "2h"           | "1h"          | -90       | 1
        "1h"           | "1h"          | -70       | 0



    }

    @Unroll
    def "test getProjectModeChangeStatus"() {

        given:
            String project = "TestProject"

            def data = [
                executions: [
                    active: isActive,
                    action: action,
                    value : '1h'
                ],
                schedule  : [
                    active: isActive,
                    action: action,
                    value : '1h'
                ]
            ]
            String jsonString = new ObjectMapper().writeValueAsString(data)

            def rundeckProject = Mock(IRundeckProject) {
                existsFileResource(_) >> true
                loadFileResource(_, _) >> { args ->
                    args[1].write(jsonString.bytes)
                    jsonString.length()
                }
            }

            def date = new Date() + 1

            def mockFrameworkService = new MockFrameworkService()
            mockFrameworkService.setRundeckProject(rundeckProject)
            service.frameworkService = mockFrameworkService
            service.quartzScheduler = Mock(Scheduler) {
                (hasDate ? 1 : 0) * getTrigger({ it.name == 'TestProject-' + type }) >> Mock(Trigger) {
                    getNextFireTime() >> date
                }
            }

        when:
            def result = service.getProjectModeChangeStatus(project, type)
        then:
            result.active == isActive
            result.action == (hasDate ? action : null)
            result.nextFireTime == (hasDate ? date : null)

        where:
            isActive | action    | type         | hasDate
            true     | 'enable'  | 'executions' | true
            true     | 'disable' | 'executions' | true
            false    | 'enable'  | 'executions' | false
            false    | 'disable' | 'executions' | false
            true     | 'enable'  | 'schedule'   | true
            true     | 'disable' | 'schedule'   | true
            false    | 'enable'  | 'schedule'   | false
            false    | 'disable' | 'schedule'   | false
    }


}

class MockScheduledExecutionService{

    boolean isScheduledRegister = false
    Scheduler scheduler

    def hasJobScheduled(String jobName, String groupName){
        isScheduledRegister
    }

    def unscheduleJobsForProject(String project,String serverUUID=null){
        scheduler.deleteJob(new org.quartz.JobKey("", ""))

    }

    def rescheduleJobs(String serverUUID = null, String project = null) {
        def trigger = PluginUtil.createTrigger("jobName", "EXECUTIONS_JOB_GROUP_NAME", new Date())
        scheduler.scheduleJob(trigger)

    }
}
