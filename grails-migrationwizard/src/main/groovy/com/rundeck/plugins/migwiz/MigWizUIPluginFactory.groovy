package com.rundeck.plugins.migwiz

import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin
import groovy.transform.CompileStatic
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean

@CompileStatic
class MigWizUIPluginFactory
        implements FactoryBean<UIPlugin>, InitializingBean, BeanNameAware {

    String beanName
    Class<?> objectType = MigWIzUIPlugin
    boolean singleton = true
    PluginRegistry pluginRegistry

    @Override
    UIPlugin getObject() throws Exception {
        def bean = new MigWIzUIPlugin()
        return bean
    }

    @Override
    void afterPropertiesSet() throws Exception {
        pluginRegistry.registerPlugin(ServiceNameConstants.UI, MigWIzUIPlugin.PROVIDER_NAME, beanName)
    }
}

