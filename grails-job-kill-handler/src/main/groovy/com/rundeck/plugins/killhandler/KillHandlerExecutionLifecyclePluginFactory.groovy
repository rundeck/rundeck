package com.rundeck.plugins.killhandler

import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.jobs.ExecutionLifecyclePlugin
import groovy.transform.CompileStatic
import org.rundeck.app.spi.AuthorizedServicesProvider
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class KillHandlerExecutionLifecyclePluginFactory
        implements FactoryBean<ExecutionLifecyclePlugin>, InitializingBean, BeanNameAware {

    @Autowired AuthorizedServicesProvider rundeckAuthorizedServicesProvider
    @Autowired KillHandlerProcessTrackingService processTrackingService
    String beanName
    Class<?> objectType = KillHandlerExecutionLifecyclePlugin
    boolean singleton = false
    PluginRegistry pluginRegistry

    @Override
    ExecutionLifecyclePlugin getObject() throws Exception {
        def bean = new KillHandlerExecutionLifecyclePlugin()
        bean.rundeckAuthorizedServicesProvider = rundeckAuthorizedServicesProvider
        bean.processTrackingService = processTrackingService
        return bean
    }

    @Override
    void afterPropertiesSet() throws Exception {
        pluginRegistry.registerPlugin(ServiceNameConstants.ExecutionLifecycle, KillHandlerExecutionLifecyclePlugin.PROVIDER_NAME, beanName)
    }
}

