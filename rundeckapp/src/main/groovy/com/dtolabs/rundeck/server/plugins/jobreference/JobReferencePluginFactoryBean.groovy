package com.dtolabs.rundeck.server.plugins.jobreference

import com.dtolabs.rundeck.server.plugins.PluginFactoryBean
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.springframework.beans.factory.annotation.Autowired
import rundeck.services.ExecutionService

@CompileStatic
class JobReferencePluginFactoryBean<T> extends PluginFactoryBean<T> {
    @Autowired ExecutionService executionService

    JobReferencePluginFactoryBean(final Class<?> objectType) {
        super(objectType)
    }

    @Override
    @CompileDynamic
    T getObject() throws Exception {
        T object = super.getObject()
        object.executionService = executionService
        return object
    }
}
