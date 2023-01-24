package rundeck.data.validation.validators.notification

import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.plugins.DescribedPlugin
import com.dtolabs.rundeck.core.plugins.PluggableProviderService
import com.dtolabs.rundeck.core.plugins.PluginRegistry
import com.dtolabs.rundeck.core.plugins.PluginServiceCapabilities
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyResolver
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import org.rundeck.app.core.FrameworkServiceCapabilities
import rundeck.data.job.RdNotification
import spock.lang.Specification

class PluginNotificationValidatorSpec extends Specification {
    def "invalid when plugin type is missing"() {
        given:
        def fwkSvc = Mock(FrameworkServiceCapabilities) {
            getPluginService() >> Mock(PluginServiceCapabilities) {
                getPluginDescriptor(_,_) >> null
            }
            getFrameworkProject(_) >> Mock(IRundeckProject) {
                getProjectProperties( ) >> [:]
            }
        }
        PluginNotificationValidator validator = new PluginNotificationValidator(fwkSvc, "proj1")

        when:
        RdNotification n = new RdNotification()
        validator.validate(n, n.errors)

        then:
        n.errors.errorCount == 1
        n.errors.fieldErrors[0].code == "scheduledExecution.notifications.pluginTypeNotFound.message"
    }

    def "invalid when plugin configuration is bad"() {
        given:
        def fwkSvc = Mock(FrameworkServiceCapabilities) {
            getPluginService() >> Mock(PluginServiceCapabilities) {
                getPluginDescriptor(_,_) >> Mock(DescribedPlugin)
                createPluggableService(_) >> Mock(PluggableProviderService)
                getPluginRegistry() >> Mock(PluginRegistry) {
                    validatePluginByName(_,_,_,_,_) >> new ValidatedPlugin(valid: false, report: new Validator.Report())
                }
            }
            getFrameworkProject(_) >> Mock(IRundeckProject) {
                getProjectProperties( ) >> [:]
            }
            getFrameworkPropertyResolverWithProps(_,_) >> Mock(PropertyResolver)
        }
        PluginNotificationValidator validator = new PluginNotificationValidator(fwkSvc, "proj1")

        when:
        RdNotification n = new RdNotification()
        validator.validate(n, n.errors)

        then:
        n.errors.errorCount == 1
        n.errors.fieldErrors[0].code == "scheduledExecution.notifications.invalidPlugin.message"
    }
}
