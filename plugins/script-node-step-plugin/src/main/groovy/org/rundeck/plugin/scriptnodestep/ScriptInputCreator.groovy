package org.rundeck.plugin.scriptnodestep


import com.dtolabs.rundeck.core.execution.ExecutionContext

/**
 * Create inputstream data for a context
 */
interface ScriptInputCreator {
    InputStream createInputForProcess(ExecutionContext context)
}