package com.dtolabs.rundeck.app.internal.framework

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.execution.service.NodeExecutorService
import com.dtolabs.rundeck.core.execution.service.NodeExecutorServiceProfile
import groovy.transform.CompileStatic
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
/**
 * Factory to create NodeExecutorService, required to lazily load injection dependencies
 */
@CompileStatic
class NodeExecutorServiceFactory implements FactoryBean<NodeExecutorService> {
    @Autowired @Lazy
    Framework rundeckFramework
    @Autowired
    NodeExecutorServiceProfile rundeckNodeExecutorProfile

    @Override
    NodeExecutorService getObject() throws Exception {
        return new NodeExecutorService(rundeckFramework, rundeckNodeExecutorProfile)
    }
    Class<?> objectType = NodeExecutorService
}
