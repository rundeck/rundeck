package com.dtolabs.rundeck.app.internal.framework

import com.dtolabs.rundeck.core.execution.service.NodeExecutor
import com.dtolabs.rundeck.core.execution.service.NodeExecutorServiceProfile
import groovy.transform.CompileStatic

/**
 * Bean class for NodeExecutorServiceProfile
 */
@CompileStatic
class RundeckNodeExecutorProfile implements NodeExecutorServiceProfile {
    String defaultLocalProvider
    String defaultRemoteProvider
    Map<String, Class<? extends NodeExecutor>> localRegistry
}
