package org.rundeck.app.data.job

import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.server.projects.RundeckProject
import com.dtolabs.rundeck.server.projects.RundeckProjectConfig
import grails.util.Holders
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.testing.GrailsUnitTest
import rundeck.services.FrameworkService
import rundeck.services.NotificationService
import spock.lang.Specification

class RdJobSpec extends Specification implements GrailsUnitTest {

    Closure doWithSpring() {
        { ->
            notificationService(InstanceFactoryBean, Mock(NotificationService))
            frameworkService(InstanceFactoryBean, Mock(FrameworkService))
        }
    }

    def setup() {
        Holders.setGrailsApplication(grailsApplication)
    }

    def "Validate"() {
        given:
        def frameworkService = applicationContext.getBean("frameworkService")
        def notificationService = applicationContext.getBean("notificationService")
        def mockValidatedPlugin = Mock(ValidatedPlugin) {
            getReport() >> Mock(Validator.Report)
        }


        when:
        RdJob rdJobData = new RdJob()
        rdJobData.id = 1L
        rdJobData.dateCreated = new Date()
        rdJobData.lastUpdated = new Date()
        rdJobData.scheduled = false
        rdJobData.project = "proj1#"
        rdJobData.jobName ="sn_-ar!@%f#blat"
//        rdJobData.notificationSet = [
//                new RdJob.RdNotificationData(type: "email", eventTrigger: "success", content:"barf@@braf.com"),
//                new RdJob.RdNotificationData(type: "url", eventTrigger: "success", content:"htttp://somewhere.com"),
//                new RdJob.RdNotificationData(type: "plugin", eventTrigger: "success", content:'{}'),
//        ]
        def valid = rdJobData.validate()
        println rdJobData.errors.errorCount
        rdJobData.errors.allErrors.each { it ->
            println it
        }
        then:
//        1 * frameworkService.getFrameworkProject("proj1") >> new RundeckProject("proj1",new RundeckProjectConfig("proj1",null,null,null,null),null)
//        1 * notificationService.getNotificationPluginDescriptor("plugin") >> Mock(DescribedPlugin)
//        1 * notificationService.validatePluginConfig(_,_,_) >> mockValidatedPlugin
        !valid
    }
}
