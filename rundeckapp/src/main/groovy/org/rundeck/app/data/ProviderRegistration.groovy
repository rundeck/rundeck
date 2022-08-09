package org.rundeck.app.data

import groovy.transform.CompileStatic
import org.rundeck.spi.data.DataManager
import org.rundeck.spi.data.DataProvider
import org.springframework.beans.factory.InitializingBean

/**
 * Bean to register specified data providers with a DataManager
 */
@CompileStatic
class ProviderRegistration implements InitializingBean {

    DataManager dataManager
    List <DataProvider<?>> providers = []

    @Override
    void afterPropertiesSet() throws Exception {
        for (def provider : providers) {
            dataManager.registerDataProvider(provider);
        }
    }
}
