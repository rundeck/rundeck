package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.logging.LogFileStorageException
import com.dtolabs.rundeck.core.plugins.configuration.Configurable
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException
import com.dtolabs.rundeck.core.plugins.configuration.Describable
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.plugins.logging.ExecutionFileStoragePlugin
import org.apache.log4j.Logger

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 6/3/13
 * Time: 2:50 PM
 */
class ScriptExecutionFileStoragePlugin implements ExecutionFileStoragePlugin, Describable, Configurable {
    static Logger logger = Logger.getLogger(ScriptExecutionFileStoragePlugin)
    Description description
    private Map<String, Closure> handlers
    Map configuration
    Map<String, ? extends Object> pluginContext

    ScriptExecutionFileStoragePlugin(Map<String, Closure> handlers, Description description) {
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
                throw new RuntimeException("ScriptExecutionFileStoragePlugin: '${it}' closure not defined for plugin ${description.name}")
            }
        }
    }

    boolean isAvailable(String filetype) throws LogFileStorageException {
        logger.debug("isAvailable(${filetype}) ${pluginContext}")
        def closure = handlers.available
        def binding = [
                configuration: configuration,
                context: pluginContext + (filetype ? [filetype: filetype] : [:])
        ]
        def result = null
        if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                result = newclos.call(filetype, binding.context , binding.configuration)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try {
                result = newclos.call(filetype, binding.context)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else {
            throw new RuntimeException("ScriptExecutionFileStoragePlugin: 'available' closure signature invalid for plugin ${description.name}, cannot open")
        }
        return result ? true : false
    }

    boolean store(String filetype, InputStream stream, long length, Date lastModified) throws IOException, LogFileStorageException {
        logger.debug("store($filetype) ${pluginContext}")
        def closure = handlers.store
        def binding = [
                configuration: configuration,
                context: pluginContext +( filetype ? [filetype: filetype] : [:]),
                stream: stream,
                length: length,
                lastModified: lastModified
        ]
        if (closure.getMaximumNumberOfParameters() == 4) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                return newclos.call(filetype, binding.context, binding.configuration, binding.stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                return newclos.call(filetype, binding.context, binding.stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try {
                return newclos.call(filetype, binding.stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else {
            throw new RuntimeException("ScriptExecutionFileStoragePlugin: 'store' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }

    @Override
    boolean retrieve(String filetype, OutputStream stream) throws IOException, LogFileStorageException {
        logger.debug("retrieve($filetype) ${pluginContext}")
        def closure = handlers.retrieve
        def binding = [
                configuration: configuration,
                context: pluginContext + (filetype ? [filetype: filetype] : [:]),
                stream: stream,
        ]
        if (closure.getMaximumNumberOfParameters() == 4) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                return newclos.call(filetype, binding.context, binding.configuration, binding.stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                return newclos.call(filetype, binding.context, binding.stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try {
                return newclos.call(filetype, binding.stream)
            } catch (Exception e) {
                throw new LogFileStorageException(e.getMessage(), e)
            }
        } else {
            throw new RuntimeException("ScriptExecutionFileStoragePlugin: 'retrieve' closure signature invalid for plugin ${description.name}, cannot open")
        }
    }


    public static boolean validAvailableClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[0] == String && closure.parameterTypes[1] == Map && closure.parameterTypes[2] == Map
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == String && closure.parameterTypes[1] == Map
        }
        return false
    }

    public static boolean validStoreClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 4) {
            return closure.parameterTypes[0] == String && closure.parameterTypes[1] == Map && closure.parameterTypes[2] == Map && closure.parameterTypes[3] == InputStream
        } else if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[0] == String && closure.parameterTypes[1] == Map && closure.parameterTypes[2] == InputStream
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == String && closure.parameterTypes[1] == Map
        }
        return false
    }

    public static boolean validRetrieveClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 4) {
            return closure.parameterTypes[0] == String && closure.parameterTypes[1] == Map && closure.parameterTypes[2] == Map && closure.parameterTypes[3] == OutputStream
        } else if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[0] == String && closure.parameterTypes[1] == Map && closure.parameterTypes[2] == OutputStream
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == String && closure.parameterTypes[1] == Map
        }
        return false
    }

    @Override
    public java.lang.String toString() {
        return "ScriptExecutionFileStoragePlugin{" +
                "description=" + description +
                ", configuration=" + configuration +
                ", pluginContext=" + pluginContext +
                '}';
    }
}
