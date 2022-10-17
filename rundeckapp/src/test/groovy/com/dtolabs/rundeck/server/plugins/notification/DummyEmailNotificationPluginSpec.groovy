package com.dtolabs.rundeck.server.plugins.notification

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import grails.testing.gorm.DataTest
import grails.testing.mixin.integration.Integration
import org.springframework.context.MessageSource
import rundeck.Notification
import rundeck.ScheduledExecution
import rundeck.services.ScheduledExecutionService
import spock.lang.Specification

class DummyEmailNotificationPluginSpec extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {
        [ScheduledExecution, Notification]
    }

    def "property validators consistent with service validations" () {
        given:
        Map<PropertyValidator, Object> pluginValidations = [
                new DummyEmailNotificationPlugin.EmailValidator() : recipients
        ]
        ScheduledExecutionService service = new ScheduledExecutionService()
        service.messageSource = Mock(MessageSource) {
            getMessage(_,_) >> null
        }
        ScheduledExecution se = new ScheduledExecution(jobName: 'testJob', project: 'testProject')
        Notification notif = new Notification(eventTrigger: "onfailure",type: "email", scheduledExecution: se)
        notif.content = recipients
        se.notifications = [ notif ]

        when:
        boolean validForPlugin = true
        for (def validationPair : pluginValidations){
            try{
                if(!validationPair.key.isValid(validationPair.value))
                    throw new Exception("invalid")
            }catch(Exception ignored){
                validForPlugin = false
                break
            }
        }

        boolean validForService = !service.validateDefinitionNotifications(se, [:], [:], [:])

        then:
        validForPlugin == validForService

        where:
        recipients                        | _
        "mail1@mail.com,  mail2@mail.com" | _
        "mail1@mail.com, asdf@"           | _
        "mail1@mail.com, mail2@mail.com"  | _
        ""                                | _
    }
}
