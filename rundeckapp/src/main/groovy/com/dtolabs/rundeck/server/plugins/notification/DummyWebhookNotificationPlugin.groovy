package com.dtolabs.rundeck.server.plugins.notification

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginMetadata
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.SelectLabels
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin


/**
 * Dummy Webhook notification provider, implementation lives in NotificationService
 */
@Plugin(name = 'url', service = ServiceNameConstants.Notification)
@PluginDescription(title = 'Send Webhook',
    description = '''Send a HTTP request to one ore more URLs.''')
@PluginMetadata(key = 'faicon', value = 'globe')
class DummyWebhookNotificationPlugin implements NotificationPlugin {
    @PluginProperty(
        title = "URL(s)",
        description = "Enter comma-separated URLs",
        required = true,
        validatorClass = WebhookUrlValidator
    )
    @RenderingOption(key = 'displayType', value = 'MULTI_LINE')
    String urls

    static class WebhookUrlValidator implements PropertyValidator{
        @Override
        boolean isValid(final String value) throws ValidationException {
            def arr = value?.split(",")
            def validCount=0
            def errs=[]
            arr?.each { String url ->
                boolean valid = false
                try {
                    def newurl=new URL(url)
                    valid=true
//                    if(newurl.protocol in ['http','https']){
//                        valid = !!newurl.host
//                    }else{
//                        valid=true
//                    }
                } catch (MalformedURLException e) {
                    valid = false
                }
                if (url && !valid) {
                    errs<< "Invalid URL: ${url}"
                }else if(url && valid){
                    validCount++
                }
            }
            if(errs){
                throw new ValidationException(
                    errs.join(', ')
                )
            }
            if(validCount<1){
                throw new ValidationException(
                    'Webhook URL cannot be blank'
                )
            }
            return true
        }
    }

    @PluginProperty(
        title = "Payload Format",
        description = "",
        required = true,
        defaultValue = 'xml'
    )
    @SelectValues(values = ['xml','json'])
    @SelectLabels(values = ['XML','JSON'])
    String format

    @PluginProperty(
        title = "Method",
        description = "HTTP method",
        required = false,
        defaultValue = 'post'
    )
    @SelectValues(values = ['get','post'])
    @SelectLabels(values = ['GET','POST'])
    String httpMethod

    @Override
    boolean postNotification(final String trigger, final Map executionData, final Map config) {
        throw new UnsupportedOperationException("Dummy plugin implementation")
    }
}
