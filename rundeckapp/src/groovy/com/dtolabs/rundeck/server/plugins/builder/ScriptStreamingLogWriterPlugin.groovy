package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.logging.LogEvent
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.logging.StreamingLogWriterPlugin
import org.apache.log4j.Logger


/**
 * Streaming Log Writer plugin implemention built from a groovy DSL
 */
class ScriptStreamingLogWriterPlugin implements StreamingLogWriterPlugin, Describable, Configurable {
    static Logger logger = Logger.getLogger(ScriptStreamingLogWriterPlugin)
    Description description
    private Map<String,Closure> handlers
    Map configuration
    private Object streamContext
    private Map pluginContext
    ScriptStreamingLogWriterPlugin(Map<String,Closure> handlers, Description description) {
        this.description=description
        this.handlers=handlers
    }

    @Override
    void configure(Properties configuration) throws ConfigurationException {
        this.configuration = new HashMap(configuration)
    }

    @Override
    void initialize(Map<String, ? extends Object> context) {
        this.pluginContext=context
    }

    @Override
    void openStream() throws IOException {
        def closure = handlers.open
        if (!closure) {
            throw new RuntimeException("LogWriterPlugin: 'open' closure not defined for plugin ${description.name}")
        }
        if (!handlers.addEvent) {
            throw new RuntimeException("LogWriterPlugin: 'addEvent' closure not defined for plugin ${description.name}")
        }
        if (!handlers.close) {
            throw new RuntimeException("LogWriterPlugin: 'close' closure not defined for plugin ${description.name}")
        }
        if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            this.streamContext = newclos.call(pluginContext, configuration)
        } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Map) {
            def Closure newclos = closure.clone()
            newclos.delegate = configuration
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            this.streamContext = newclos.call(pluginContext)
        } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
            def Closure newclos = closure.clone()
            newclos.delegate = [execution: pluginContext, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            this.streamContext = newclos.call(pluginContext)
        } else {
            logger.error("LogWriterPlugin: 'open' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }

    @Override
    synchronized void addEvent(LogEvent event) {
        def closure = handlers.addEvent
        if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(this.streamContext, event)
        } else if (closure.getMaximumNumberOfParameters() == 1 ) {
            def Closure newclos = closure.clone()
            newclos.delegate = [context: this.streamContext, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.call(event)
        }  else {
            logger.error("LogWriterPlugin: 'addEvent' closure signature invalid for plugin ${description.name}, cannot addEvent")
        }
    }

    @Override
    void close() {
        def closure = handlers.close
        if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            newclos.delegate = [context: this.streamContext, configuration: configuration]
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try {
                newclos.call(this.streamContext)
            } catch (IOException e) {
                logger.error("LogWriterPlugin: 'close' for plugin ${description.name}: "+e.message,e)
            }
        } else {
            logger.error("LogWriterPlugin: 'open' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }

    public static boolean validOpenClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == Map && closure.parameterTypes[1] == Map
        }else if (closure.getMaximumNumberOfParameters() == 1) {
            return closure.parameterTypes[0] == Map || closure.parameterTypes[0] == Object
        }
        return false
    }

    public static boolean validEventClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[1] == LogEvent
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return closure.parameterTypes[0] == LogEvent || closure.parameterTypes[0] == Object
        }
        return false
    }
    public static boolean validCloseClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 1) {
            return true
        }
        return false
    }
}
