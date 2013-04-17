package com.dtolabs.rundeck.server.plugins.builder

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
class ScriptNotificationPlugin implements NotificationPlugin, Describable {
    static Logger logger = Logger.getLogger(ScriptNotificationPlugin)
    Map<String, Closure> triggers
    private Description description;

    ScriptNotificationPlugin(Map<String, Closure> triggers, Description description) {
        this.triggers = triggers
        this.description = description
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
    boolean postNotification(String trigger, Map executionData, Map config) {
        def closure = triggers[trigger]
        if (closure) {
            if (closure.getMaximumNumberOfParameters() == 3) {
                return closure.call(trigger, executionData, config)
            } else if (closure.getMaximumNumberOfParameters() == 2) {
                return closure.call(executionData, config)
            } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Map) {
                def Closure newclos = closure.clone()
                newclos.delegate = config
                return newclos.call(executionData)
            } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
                def Closure newclos = closure.clone()
                newclos.delegate = [config: config, execution: executionData, trigger: trigger]
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
