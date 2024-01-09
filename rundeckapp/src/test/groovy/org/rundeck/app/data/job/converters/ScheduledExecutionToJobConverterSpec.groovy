package org.rundeck.app.data.job.converters

import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigData
import grails.testing.gorm.DataTest
import grails.util.Holders
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import org.rundeck.app.components.RundeckJobDefinitionManager
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl
import rundeck.CommandExec
import rundeck.Execution
import rundeck.Option
import rundeck.ScheduledExecution
import rundeck.Workflow
import rundeck.data.constants.WorkflowStepConstants
import rundeck.data.execution.RdExecution
import rundeck.data.job.RdWorkflow
import rundeck.data.job.RdWorkflowStep
import rundeck.services.FrameworkService
import rundeck.services.UserService
import spock.lang.Specification
import testhelper.TestDomainFactory

class ScheduledExecutionToJobConverterSpec extends Specification implements GrailsUnitTest, DataTest {

    Closure doWithSpring() {
        { ->
            userService(InstanceFactoryBean, Mock(UserService))
            frameworkService(InstanceFactoryBean, Mock(FrameworkService))
            rundeckJobDefinitionManager(InstanceFactoryBean, Mock(RundeckJobDefinitionManager))
        }
    }

    def setup() {
        Holders.setGrailsApplication(grailsApplication)
    }

    def setupSpec() {
        mockDomains(Execution, Option, ScheduledExecution, Workflow, CommandExec)
    }

    def "ConvertJob"() {

        given:
        ScheduledExecution job = TestDomainFactory.createJob()
        job.filter = "tag: unix"
        job.filterExclude = "tag: oracle"
        def opt1 = new Option(name:"opt1",optionType: "text",valuesUrlLong: URI.create("http://someremoteurlprovider/values").toURL())
        def cfgData = new JobOptionConfigData()
        cfgData.addConfig(new JobOptionConfigRemoteUrl(username:"bob"))
        opt1.setOptionConfigData(cfgData)
        job.addToOptions(opt1)

        when:
        JobData jobData = ScheduledExecutionToJobConverter.convert(job)

        then:
        jobData.uuid == job.uuid
        jobData.jobName == job.jobName
        jobData.description == job.description
        jobData.project == job.project
        jobData.argString == job.argString
        jobData.user == job.user
        jobData.timeout == job.timeout
        jobData.retry == job.retry
        jobData.retryDelay == job.retryDelay
        jobData.groupPath == job.groupPath
        jobData.userRoles == job.userRoles
        jobData.scheduled == job.scheduled
        jobData.scheduleEnabled == job.scheduleEnabled
        jobData.executionEnabled == job.executionEnabled
        jobData.multipleExecutions == job.multipleExecutions
        jobData.notifyAvgDurationThreshold == job.notifyAvgDurationThreshold
        jobData.timeZone == job.timeZone
        jobData.defaultTab == job.defaultTab
        jobData.maxMultipleExecutions == job.maxMultipleExecutions
        jobData.dateCreated == job.dateCreated
        jobData.lastUpdated == job.lastUpdated
        jobData.logConfig.loglevel == job.logConfig.loglevel
        jobData.logConfig.logOutputThreshold == job.logConfig.logOutputThreshold
        jobData.logConfig.logOutputThresholdAction == job.logConfig.logOutputThresholdAction
        jobData.logConfig.logOutputThresholdStatus == job.logConfig.logOutputThresholdStatus

        jobData.nodeConfig.filter == job.nodeConfig.filter
        jobData.nodeConfig.filterExclude == job.nodeConfig.filterExclude

        jobData.optionSet.size() == job.optionSet.size()
        jobData.optionSet[0].name == job.optionSet[0].name
        jobData.optionSet[0].optionType == job.optionSet[0].optionType
        jobData.optionSet[0].realValuesUrl == job.optionSet[0].realValuesUrl
        jobData.optionSet[0].configMap["remote-url"].username == job.options[0].optionConfigData.getJobOptionEntry(JobOptionConfigRemoteUrl).username

        jobData.workflow.steps.size() == job.workflow.steps.size()
        for(int stepIdx = 0; stepIdx < job.workflow.steps.size(); stepIdx++) {
            def step = job.workflow.steps[stepIdx]
            def convertedStep = jobData.workflow.steps[stepIdx]
            convertedStep.pluginType == step.pluginType
            convertedStep.configuration == step.configuration
        }

    }
}
