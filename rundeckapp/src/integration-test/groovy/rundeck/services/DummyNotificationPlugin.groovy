package rundeck.services

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.DynamicProperties
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin
import org.rundeck.app.spi.Services
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Value

@Plugin(name = PROVIDER_NAME, service = ServiceNameConstants.Notification)
@PluginDescription(title = "DummyNotificationPlugin", description = "DummyNotificationPlugin")
class DummyNotificationPlugin implements NotificationPlugin, DynamicProperties,  InitializingBean{
    static final String PROVIDER_NAME = 'dummy-notification-plugin'
    static final String BEAN_NAME = "dummyNotificationPlugin"

    @Value('${local.server.port}')
    String serverPort

    @Override
    boolean postNotification(String trigger, Map executionData, Map config) {
        return false
    }

    @Override
    Map<String, Object> dynamicProperties(Map<String, Object> projectAndFrameworkValues, Services services) {

        try{
            KeyStorageTree storageTree = services.getService(KeyStorageTree.class)
            String password = storageTree.readPassword("key/password")
        }catch(Exception e){
            println(e.message)
        }

        return [:]
    }

    @Override
    void afterPropertiesSet() throws Exception {
    }
}