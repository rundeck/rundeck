package org.rundeck.app.data

import groovy.transform.CompileStatic
import org.rundeck.spi.data.DataManager
import org.springframework.beans.factory.InitializingBean

/**
 * Bean to register specified data providers with a DataManager
 */
@CompileStatic
class ProviderRegistration implements InitializingBean {

    DataManager dataManager
    def providers = []

    @Override
    void afterPropertiesSet() throws Exception {
        for (def provider : providers) {
            Class<?> clazz = provider.getClass().getInterfaces() ? provider.getClass().getInterfaces()[0] :
                                    provider.getClass()
            dataManager.registerDataProvider(clazz as Class, provider);
        }
    }
}
