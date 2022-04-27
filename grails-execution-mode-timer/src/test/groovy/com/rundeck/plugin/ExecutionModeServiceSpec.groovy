package com.rundeck.plugin

import com.fasterxml.jackson.databind.ObjectMapper
import grails.converters.JSON
import grails.testing.services.ServiceUnitTest
import org.quartz.Scheduler
import org.quartz.Trigger
import spock.lang.Specification
import spock.lang.Unroll

import java.text.DateFormat
import java.text.SimpleDateFormat

class ExecutionModeServiceSpec extends Specification implements ServiceUnitTest<ExecutionModeService>{

    def "test simple saveExecutionLaterSettings empty data"(){
        given:

        Map config = [activeLater: false, activeLaterValue:null, passiveLater:false,passiveLaterValue:null  ]

        service.executionService=new MockExecutionService(executionsAreActive:false)
        service.configStorageService = new MockConfigStorageService()

        when:
        def result = service.saveExecutionModeLater(config)

        then:
        result == false

    }

    def "test simple saveExecutionLaterSettings save data"(){
        given:

        Map config = [activeLater: true, activeLaterValue:"10h", passiveLater:false,passiveLaterValue:null  ]

        service.executionService=new MockExecutionService(executionsAreActive:false)
        service.configStorageService = new MockConfigStorageService()
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            1*scheduleJob(_,_)
        }

        when:
        def result = service.saveExecutionModeLater(config)

        then:
        result == true

    }

    def "test simple saveExecutionLaterSettings save withData"(){
        given:

        Map config = [activeLater: false, activeLaterValue:null, passiveLater:true, passiveLaterValue:"10h"  ]

        DateFormat dateFormat = new SimpleDateFormat(UpdateModeProjectService.DATE_FORMAT);
        Date date = new Date()
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.MINUTE, -90)

        def executionValue = "1h"

        def savedDate = dateFormat.format(calendar.getTime())
        String savedData='{"active":false, "action":"disable", "value":"'+executionValue+'","dateSaved":"'+savedDate+'","executionsAreActive":true}'

        service.executionService=new MockExecutionService(executionsAreActive:true)
        service.configStorageService = new MockConfigStorageService(isFileExists: true, data: savedData)
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            1*scheduleJob(_,_)
        }

        when:
        def result = service.saveExecutionModeLater(config)

        then:
        result == true

    }

    def "test saveExecutionLaterSettings without change"(){
        given:

        Map config = [activeLater: activeLater, activeLaterValue:activeLaterValue, passiveLater:false, passiveLaterValue: null]

        DateFormat dateFormat = new SimpleDateFormat(UpdateModeProjectService.DATE_FORMAT);
        Date date = new Date()
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.MINUTE, -90)

        def executionValue = "10h"

        def savedDate = dateFormat.format(calendar.getTime())
        String savedData='{"active":false, "action":"enable", "value":"'+executionValue+'","dateSaved":"'+savedDate+'","executionsAreActive":'+savedExecutionMode+'}'

        service.executionService=new MockExecutionService(executionsAreActive:executionMode)
        service.configStorageService = new MockConfigStorageService(isFileExists: true, data: savedData)
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        service.quartzScheduler = Mock(Scheduler){
            scheduledCalls*scheduleJob(_,_)
            removeCalls*getTrigger(_) >> Mock(org.quartz.Trigger)
            removeCalls*deleteJob(_)
        }

        when:
        def result = service.saveExecutionModeLater(config)

        then:
        result == resultValue

        where:
        executionMode | savedExecutionMode | activeLater | activeLaterValue | resultValue | scheduledCalls | removeCalls
        false         | false              | true        | "12h"            | true        | 1              | 0
        false         | true               | true        | "12h"            | true        | 1              | 1
        true          | true               | false       | "10h"            | false       | 0              | 0
        true          | false              | false       | "10h"            | true        | 0              | 1


    }

    def "test initProcess"() {
        given:
        DateFormat dateFormat = new SimpleDateFormat(UpdateModeProjectService.DATE_FORMAT);
        Date date = new Date()
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.MINUTE, -90)

        def savedDate = dateFormat.format(calendar.getTime())
        String savedData='{"active":'+active+', "action":"disable", "value":"'+executionValue+'","dateSaved":"'+savedDate+'","executionsAreActive":true}'

        service.configStorageService = new MockConfigStorageService(isFileExists: true, data: savedData)
        service.quartzScheduler = Mock(Scheduler)
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        when:
        service.initProcess()

        then:
        scheduleCalls*service.quartzScheduler.scheduleJob(_,_)


        where:
        active      | executionValue | savedTime | scheduleCalls
        true        | "2h"           | -110      | 1
        true        | "1h"           | -90       | 0
        false       | "1h"           | -70       | 0
    }

    def "test initProcess null data"() {
        given:
        DateFormat dateFormat = new SimpleDateFormat(UpdateModeProjectService.DATE_FORMAT);
        Date date = new Date()
        Calendar calendar = Calendar.getInstance()
        calendar.setTime(date)
        calendar.add(Calendar.MINUTE, -90)

        String savedData='bad-data'

        service.configStorageService = new MockConfigStorageService(isFileExists: true, data: savedData)
        service.quartzScheduler = Mock(Scheduler)
        service.scheduledExecutionService  = new MockScheduledExecutionService(isScheduledRegister: false)

        when:
        service.initProcess()

        then:
        0*service.quartzScheduler.scheduleJob(_,_)

    }


    @Unroll
    def "test getSystemModeChangeStatus"(){
        given:
            DateFormat dateFormat = new SimpleDateFormat(UpdateModeProjectService.DATE_FORMAT);
            Date date = new Date()
            Calendar calendar = Calendar.getInstance()
            calendar.setTime(date)
            calendar.add(Calendar.MINUTE, -90)

            def executionValue = "10h"

            def savedDate = dateFormat.format(calendar.getTime())
            def data = [active: isActive, action: action, value: executionValue, dateSaved: savedDate, executionsAreActive: false]
            def json= new ObjectMapper().writeValueAsString(data)

            service.configStorageService = new MockConfigStorageService(isFileExists: true, data: json)
            service.quartzScheduler = Mock(Scheduler){
                (hasDate?1:0)*getTrigger(_) >> Mock(Trigger){
                    getNextFireTime()>>date
                }
            }

        when:
            def result = service.getSystemModeChangeStatus()
        then:
            result.active==isActive
            result.action==(hasDate?action:null)
            result.nextFireTime==(hasDate?date:null)

        where:
            isActive | action    | hasDate
            true     | 'enable'  | true
            true     | 'disable' | true
            false    | 'enable'  | false
            false    | 'disable' | false
    }

}

class MockExecutionService{
    boolean executionsAreActive=false

}

class MockConfigStorageService{

    boolean isFileExists=false
    boolean fileResourceData=null
    String data=""

    def existsFileResource(String path){
        isFileExists
    }


    def getFileResource(String path) {
        if(isFileExists){
            InputStream inputStream = new ByteArrayInputStream(data.getBytes("UTF-8"));

           return new Resource(contents: new Content(inputStream: inputStream))
        }

    }

    def writeFileResource(String path, InputStream input, Map<String, String> meta) {

    }

}

class Resource{
    Content contents
}

class Content{
    InputStream inputStream
}
