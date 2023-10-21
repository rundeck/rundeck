package com.rundeck.plugins.killhandler

import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.logging.LogFilterPlugin
import groovy.transform.CompileStatic
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class KillHandlerLogFilterPluginFactory
        implements FactoryBean<LogFilterPlugin>, InitializingBean, BeanNameAware {

    @Autowired KillHandlerProcessTrackingService processTrackingService
    String beanName
    Class<?> objectType = KillHandlerLogFilterPlugin
    boolean singleton = false
    PluginRegistry pluginRegistry

    @Override
    LogFilterPlugin getObject() throws Exception {
        def bean = new KillHandlerLogFilterPlugin()
        bean.processTrackingService = processTrackingService
        return bean
    }

    @Override
    void afterPropertiesSet() throws Exception {
        pluginRegistry.registerPlugin(ServiceNameConstants.LogFilter, KillHandlerLogFilterPlugin.PROVIDER_NAME, beanName)
    }
}

