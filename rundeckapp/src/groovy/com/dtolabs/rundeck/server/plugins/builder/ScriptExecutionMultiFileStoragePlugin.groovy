package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.logging.ExecutionFileStorageException
import com.dtolabs.rundeck.core.logging.ExecutionMultiFileStorage
import com.dtolabs.rundeck.core.logging.MultiFileStorageRequest
import com.dtolabs.rundeck.core.plugins.configuration.Description

/**
 * Extends ExecutionFileStoragePlugin to use ExecutionMultiFileStorage
 */
class ScriptExecutionMultiFileStoragePlugin extends ScriptExecutionFileStoragePlugin
        implements ExecutionMultiFileStorage {

    ScriptExecutionMultiFileStoragePlugin(final Map<String, Closure> handlers, final Description description) {
        super(handlers, description)
    }

    @Override
    void initialize(final Map<String, ? extends Object> context) {
        super.initialize(context)
        this.storeSupported = handlers['storeMultiple'] ? true : false
    }

    @Override
    boolean store(final String filetype, final InputStream stream, final long length, final Date lastModified)
            throws IOException, ExecutionFileStorageException
    {
        if (!storeSupported) {
            throw new IllegalStateException("store is not supported")
        }
        throw new IllegalStateException("Expected storeMultiple, not store")
    }

    @Override
    void storeMultiple(final MultiFileStorageRequest files) throws IOException, ExecutionFileStorageException {
        if (!storeSupported) {
            throw new IllegalStateException("store is not supported")
        }
        logger.debug("storeMultiple($files) ${pluginContext}")
        def closure = handlers.storeMultiple
        def binding = [
                configuration: configuration,
                context      : pluginContext + (files ? [files: files] : [:]),
                files        : files
        ]
        if (closure.getMaximumNumberOfParameters() == 3) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                newclos.call(files, binding.context, binding.configuration)
            } catch (Exception e) {
                throw new ExecutionFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            def Closure newclos = closure.clone()
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            newclos.delegate = binding
            try {
                newclos.call(files, binding.context)
            } catch (Exception e) {
                throw new ExecutionFileStorageException(e.getMessage(), e)
            }
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            def Closure newclos = closure.clone()
            newclos.delegate = binding
            newclos.resolveStrategy = Closure.DELEGATE_ONLY
            try {
                newclos.call(files)
            } catch (Exception e) {
                throw new ExecutionFileStorageException(e.getMessage(), e)
            }
        } else {
            throw new RuntimeException(
                    "ScriptExecutionFileStoragePlugin: 'store' closure signature invalid for plugin ${description.name}, cannot open"
            )
        }
    }

    public static boolean validStoreMultipleClosure(Closure closure) {
        if (closure.getMaximumNumberOfParameters() == 3) {
            return closure.parameterTypes[0] == MultiFileStorageRequest &&
                    closure.parameterTypes[1] == Map &&
                    closure.parameterTypes[2] == Map
        } else if (closure.getMaximumNumberOfParameters() == 2) {
            return closure.parameterTypes[0] == MultiFileStorageRequest && closure.parameterTypes[1] == Map
        } else if (closure.getMaximumNumberOfParameters() == 1) {
            return closure.parameterTypes[0] == MultiFileStorageRequest
        }
        return false
    }
}
