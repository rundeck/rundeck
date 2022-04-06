package com.rundeck.plugin.ui

import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean

class UIEnableExecutionLaterFactory implements FactoryBean<UIPlugin>, InitializingBean, BeanNameAware {
    String beanName
    Class<?> objectType = UIEnableExecutionLater
    boolean singleton = true
    PluginRegistry pluginRegistry

    @Override
    UIPlugin getObject() throws Exception {
        def bean = new UIEnableExecutionLater()
        return bean
    }

    @Override
    void afterPropertiesSet() throws Exception {
        pluginRegistry.registerPlugin(ServiceNameConstants.UI, UIEnableExecutionLater.PROVIDER_NAME, beanName)
    }
}
