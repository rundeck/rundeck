package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.logging.LogFileState
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.logging.LogFileStoragePlugin
import org.apache.log4j.Logger

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 6/3/13
 * Time: 2:50 PM
 */
class ScriptLogFileStoragePlugin implements LogFileStoragePlugin, Describable{
    static Logger logger = Logger.getLogger(ScriptLogFileStoragePlugin)
    Description description
    private Map<String, Closure> handlers
    Map configuration
    Map<String, ? extends Object> pluginContext

    ScriptLogFileStoragePlugin(Map<String, Closure> handlers, Description description) {
        this.handlers = handlers
        this.description = description
    }


    @Override
    void initialize(Map<String, ? extends Object> context) {
        this.pluginContext = context
        ['state','retrieve','store'].each {
            if (!handlers[it]) {
                throw new RuntimeException("ScriptLogFileStoragePlugin: '${it}' closure not defined for plugin ${description.name}")
            }
        }
    }

    @Override
    LogFileState getState() {
        logger.debug("getState ${pluginContext}")
        def closure = handlers.state
        def binding= [
                configuration: configuration,
                context: pluginContext
        ]
        LogFileState.values().each { binding[it.toString()]=it }
        if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            return newclos.call(pluginContext, configuration)
        } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            return newclos.call(pluginContext)
        } else {
            throw new RuntimeException("ScriptLogFileStoragePlugin: 'state' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }

    @Override
    boolean storeLogFile(InputStream stream) throws IOException {
        logger.debug("storeLogFile ${pluginContext}")
        def closure = handlers.store
        def binding = [
                configuration: configuration,
                context: pluginContext,
                stream: stream,
        ]
        if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            return newclos.call(pluginContext, configuration, stream)
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            return newclos.call(pluginContext, stream)
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            return newclos.call(stream)
        } else {
            throw new RuntimeException("ScriptLogFileStoragePlugin: 'store' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }

    @Override
    boolean retrieveLogFile(OutputStream stream) throws IOException {
        logger.debug("retrieveLogFile ${pluginContext}")
        def closure = handlers.retrieve
        def binding = [
                configuration: configuration,
                context: pluginContext,
                stream: stream,
        ]
        if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            return newclos.call(pluginContext, configuration, stream)
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            return newclos.call(pluginContext, stream)
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            return newclos.call(stream)
        } else {
            throw new RuntimeException("ScriptLogFileStoragePlugin: 'retrieve' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }


    public static boolean validStateClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == Map
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return (closure.parameterTypes[0] in [Map]) || closure.parameterTypes[0] == Object
        }
        return false
    }

    public static boolean validStoreClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == Map && closure.parameterTypes[2] == InputStream
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == InputStream
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return true
        }
        return false
    }
    public static boolean validRetrieveClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == Map && closure.parameterTypes[2] == OutputStream
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == OutputStream
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return true
        }
        return false
    }

    @Override
    public java.lang.String toString() {
        return "ScriptLogFileStoragePlugin{" +
                "description=" + description +
                ", configuration=" + configuration +
                ", pluginContext=" + pluginContext +
                '}';
    }
}
