package rundeck.controllers

import com.dtolabs.rundeck.core.VersionConstants
import com.dtolabs.rundeck.core.common.IFramework
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.config.ConfigService
import rundeck.services.FrameworkService
import spock.lang.Specification

import java.nio.file.Files

class MenuServiceSpec extends Specification implements ServiceUnitTest<MenuService> {
    def setup() {
        service.configurationService = Mock(ConfigService)
        service.frameworkService = Mock(FrameworkService)
    }

    def "should show first run disabled"() {
        when:
            def result = service.shouldShowFirstRunInfo()

        then:
            !result
            1 * service.configurationService.getBoolean(MenuService.SYS_CONFIG_DETECT_FIRST_RUN, true) >> false
            0 * service.frameworkService.getRundeckFramework()
    }

    def "should show first run enabled, does not exist"() {
        given:
            def tmpdir = Files.createTempDirectory("MenuServiceSpec")
            def fwk = Mock(IFramework) {
                _ * getPropertyLookup() >> Mock(IPropertyLookup) {
                    _ * getProperty('framework.var.dir') >> tmpdir.toFile().absolutePath
                    _ * hasProperty('framework.var.dir') >> true
                }
            }
            service.frameworkService = Mock(FrameworkService) {
                _ * getRundeckFramework() >> fwk
            }
            def File firstrun = new File(tmpdir.toFile(),".first-run-${VersionConstants.VERSION}")

            assert !firstrun.exists()
        when:
            def result = service.shouldShowFirstRunInfo()

        then:
            result
            1 * service.configurationService.getBoolean(MenuService.SYS_CONFIG_DETECT_FIRST_RUN, true) >> true
            firstrun.exists()
        cleanup:
            //remove files
            firstrun.delete()
            Files.delete(tmpdir)

    }
    def "should show first run enabled, does exist"() {
        given:
            def tmpdir = Files.createTempDirectory("MenuServiceSpec")
            def fwk = Mock(IFramework) {
                _ * getPropertyLookup() >> Mock(IPropertyLookup) {
                    _ * getProperty('framework.var.dir') >> tmpdir.toFile().absolutePath
                    _ * hasProperty('framework.var.dir') >> true
                }
            }
            service.frameworkService = Mock(FrameworkService) {
                _ * getRundeckFramework() >> fwk
            }
            def File firstrun = new File(tmpdir.toFile(),".first-run-${VersionConstants.VERSION}")

            assert firstrun.createNewFile()
        when:
            def result = service.shouldShowFirstRunInfo()

        then:
            !result
            1 * service.configurationService.getBoolean(MenuService.SYS_CONFIG_DETECT_FIRST_RUN, true) >> true
            firstrun.exists()
        cleanup:
            firstrun.delete()
            Files.delete(tmpdir)

    }
}
