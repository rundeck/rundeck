package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.logging.LogFileStorageException
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
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
class ScriptLogFileStoragePlugin implements LogFileStoragePlugin, Describable, Configurable {
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
    void configure(Properties configuration) throws ConfigurationException {
        this.configuration = new HashMap(configuration)
    }


    @Override
    void initialize(Map<String, ? extends Object> context) {
        this.pluginContext = context
        ['available', 'retrieve', 'store'].each {
            if (!handlers[it]) {
                throw new RuntimeException("ScriptLogFileStoragePlugin: '${it}' closure not defined for plugin ${description.name}")
            }
        }
    }

    @Override
    boolean isAvailable() throws LogFileStorageException {
        logger.debug("isAvailable ${pluginContext}")
        def closure = handlers.available
        def binding = [
                configuration: configuration,
                context: pluginContext
        ]
        def result=null
        if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                result= newclos.call(pluginContext, configuration)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 1 && closure.parameterTypes[0] == Object) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try {
                result = newclos.call(pluginContext)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else {
            throw new RuntimeException("ScriptLogFileStoragePlugin: 'available' closure signature invalid for plugin ${description.name}, cannot open")
        }
        return result ? true : false
    }

    @Override
    boolean store(InputStream stream, long length, Date lastModified) throws IOException, LogFileStorageException {
        logger.debug("store ${pluginContext}")
        def closure = handlers.store
        def binding = [
                configuration: configuration,
                context: pluginContext,
                stream: stream,
                length:length,
                lastModified:lastModified
        ]
        if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                return newclos.call(pluginContext, configuration, stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                return newclos.call(pluginContext, stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try{
                return newclos.call(stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else {
            throw new RuntimeException("ScriptLogFileStoragePlugin: 'store' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }

    @Override
    boolean retrieve(OutputStream stream) throws IOException,LogFileStorageException {
        logger.debug("retrieve ${pluginContext}")
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
            try{
                return newclos.call(pluginContext, configuration, stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try{
                return newclos.call(pluginContext, stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try{
                return newclos.call(stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else {
            throw new RuntimeException("ScriptLogFileStoragePlugin: 'retrieve' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }


    public static boolean validAvailableClosure(Closure closure) {
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
