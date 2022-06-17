package org.rundeck.app.data

import groovy.transform.CompileStatic
import org.rundeck.spi.data.AccessContextProvider
import org.rundeck.spi.data.ContextDataProvider
import org.rundeck.spi.data.DataManager
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired

/**
 * Bean to register specified data providers with a DataManager
 */
@CompileStatic
class ProviderRegistration implements InitializingBean {

    DataManager dataManager
    AccessContextProvider<?> accessContextProvider
    List<ContextDataProvider<?, ?, ?>> providers = []

    @Override
    void afterPropertiesSet() throws Exception {
        for (ContextDataProvider<?, ?, ?> provider : providers) {
            dataManager.registerDataProvider(
                provider,
                accessContextProvider
            )
        }
    }
}
