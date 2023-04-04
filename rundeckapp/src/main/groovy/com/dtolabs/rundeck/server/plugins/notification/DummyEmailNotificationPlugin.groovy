package com.dtolabs.rundeck.server.plugins.notification

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginMetadata
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import rundeck.data.validation.validators.AnyDomainEmailValidator


/**
 * Provides descriptor of the built-in Email notification functionality. Current implementation
 * lives in NotificationServicee
 */
@Plugin(name = 'email', service = ServiceNameConstants.Notification)
@PluginDescription(title = 'Send Email',
    description = '''Send an email to multiple recipients, and customize the subject line.''')
@PluginMetadata(key = 'faicon', value = 'envelope')
class DummyEmailNotificationPlugin implements NotificationPlugin {
    @PluginProperty(
        title = 'To',
        description = '''comma-separated email addresses. You can substitute these variables: ${job.user.name}, ${job.user.email}''',
        required = true,validatorClass = EmailValidator)

    String recipients
    static class EmailValidator implements PropertyValidator{
        @Override
        boolean isValid(final String value) throws ValidationException {
            def arr = value?.split(",")
            boolean failed=false
            def validator = new AnyDomainEmailValidator()
            def validcount=0
            def errs=[]
            arr?.each { email ->
                if(email && email.indexOf('${')>=0){
                    //don't reject embedded prop refs
                    validcount++
                }else if (email && !validator.isValid(email)) {
                    failed = true
                    errs << "Invalid email address: ${email}"
                }else if(email){
                    validcount++
                }
            }
            if(errs){
                throw new ValidationException(errs.join(', '))
            }
            if(validcount<1){
                throw new ValidationException('Cannot be blank')
            }
            return true
        }
    }

    @PluginProperty(
        title = "Subject",
        description = '''Template for the email subject. Can contain property references: ${group.key}

A default template of `${notification.eventStatus} [${execution.project}] ${job.group}/${job.name} ${execution.argstring}}` will be used unless 
overridden in the configuration file.

See [Documentation](https://docs.rundeck.com/docs/administration/configuration/email-settings.html#custom-email-templates)'''
    )
    String subject
    @PluginProperty(
        title = 'Include log output',
        description = 'Note: Log output can only be included for success or failure triggers.'
    )
    Boolean attachLog

    @PluginProperty(
        title = 'Attachment type',
        defaultValue = 'file'
    )
    @SelectValues(values = [
        'file',
        'inline'
    ])
    @SelectLabels(values = [
        'Attached as file to email',
        'Inline to email'
    ])
    String attachType

    @Override
    boolean postNotification(final String trigger, final Map executionData, final Map config) {
        throw new UnsupportedOperationException("Dummy plugin implementation")
    }
}
