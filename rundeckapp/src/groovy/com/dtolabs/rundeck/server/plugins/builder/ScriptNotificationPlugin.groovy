package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import org.apache.log4j.Logger

/**
 * ScriptNotificationPlugin is a {@link NotificationPlugin} implementation based on a set
 * of groovy closures acting as the triggers, which is built by {@link ScriptNotificationPluginBuilder}.
 * User: greg
 * Date: 4/16/13
 * Time: 4:40 PM
 */
class ScriptNotificationPlugin implements NotificationPlugin, Describable, Configurable {
    static Logger logger = Logger.getLogger(ScriptNotificationPlugin)
    Map<String, Closure> triggers
    Map configuration
    private Description description;

    ScriptNotificationPlugin(Map<String, Closure> triggers, Description description) {
        this.triggers = triggers
        this.description = description
    }

    @Override
    void configure(Properties configuration) throws ConfigurationException {
        this.configuration = new HashMap(configuration)
    }
    public static boolean validNotificationClosure(Closure closure){
        if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[0]==String && closure.parameterTypes[1] == Map && closure.parameterTypes[2] == Map
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == Map
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return closure.parameterTypes[0] == Map || closure.parameterTypes[0] == Object
        }
        return false
    }

    @Override
    boolean postNotification(String trigger, Map executionData, Map ignored) {
        //nb: we can ignore the map of extra configuration properties, because all config properties will
        //be set via the Configurable interface.
        def closure = triggers[trigger]
        if (closure) {
            if (closure.getMaximumNumberOfParameters() == 3) {
                return closure.call(trigger, executionData, this.configuration)
            } else if (closure.getMaximumNumberOfParameters() == 2) {
                return closure.call(executionData, this.configuration)
            } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Map) {
                def Closure newclos = closure.clone()
                newclos.delegate = this.configuration
                newclos.resolveStrategy = Closure.DELEGATE_ONLY
                return newclos.call(executionData)
            } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
                def Closure newclos = closure.clone()
                newclos.delegate = [configuration: this.configuration, execution: executionData, trigger: trigger]
                newclos.resolveStrategy=Closure.DELEGATE_ONLY
                return newclos.call(executionData)
            } else {
                logger.error("Trigger \"on${trigger}\" signature invalid for plugin ${description.name}, notification not sent")
            }
        } else {
            logger.error("Trigger \"on${trigger}\" not defined for plugin ${description.name}, notification not sent")
        }
        return false
    }

    @Override
    Description getDescription() {
        return description;
    }
}
