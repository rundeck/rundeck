package rundeck.interceptors

import com.codahale.metrics.MetricRegistry
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.web.gsp.io.CachingGrailsConventionGroovyPageLocator
import org.grails.web.servlet.view.GroovyPageViewResolver
import rundeck.controllers.ApiController
import rundeck.services.ApiService
import rundeck.services.ConfigurationService
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Test the RefererInterceptor, using ApiController for mocked controller requests
 * @author greg
 * @since 4/4/17
 */
@TestFor(ApiController)
@Mock([AA_TimerInterceptor, ApiAccessInterceptor, RefererInterceptor])
class RefererInterceptorSpec extends Specification implements InterceptorUnitTest<RefererInterceptor> {

    static doWithSpring = {
        configurationService(ConfigurationService)
        metricRegistry(MetricRegistry)

        //pageTemplateEngine,pageLocator,viewResolver, allows mocked filter to call `render(view:'/view')`
        pageTemplateEngine(GroovyPagesTemplateEngine)
        pageLocator(CachingGrailsConventionGroovyPageLocator)
        viewResolver(GroovyPageViewResolver, pageTemplateEngine, pageLocator)
    }

    @Unroll
    def "referer header filters api allowed #allowed"() {
        given:
        controller.configurationService.grailsApplication = grailsApplication
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.csrf.referer.filterMethod = '*'
        grailsApplication.config.rundeck.security.csrf.referer.allowApi = allowed
        grailsApplication.config.grails.serverURL = 'http://hostname:4000/rundeck'
        controller.apiService = Mock(ApiService)
        params.api_version = "18"
        request.api_version = 18
        request.forwardURI = '/api/18/tokens'
        request.method = 'POST'
        if (referer) {
            request.addHeader('referer', referer)
        }

        when:
        withFilters(action: "apiTokenRemoveExpired", controller: "api") {
            controller.apiTokenRemoveExpired()
        }

        then:
        response.status == status
        controller.configurationService != null
        if (status == 200) {
            1 * controller.apiService.requireVersion(*_) >> false
        }
        0 * controller.apiService._(*_)

        where:
        allowed | referer                        | status
        'true'  | 'http://hostname:4000/rundeck' | 200
        'true'  | 'http://wrong:4000/rundeck'    | 200
        'true'  | null                           | 200
        'false' | 'http://hostname:4000/rundeck' | 200
        'false' | 'http://wrong:4000/rundeck'    | 401
        'false' | null                           | 401
    }

    @Unroll
    def "referer header filters for #filterMethod with method #method ref #referer is #status"() {
        given:
        controller.configurationService.grailsApplication = grailsApplication
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.csrf.referer.filterMethod = filterMethod
        grailsApplication.config.rundeck.security.csrf.referer.allowApi = false
        grailsApplication.config.grails.serverURL = 'http://hostname:4000/rundeck'
        controller.apiService = Mock(ApiService)
        params.api_version = "18"
        request.api_version = 18
        request.forwardURI = '/api/18/tokens'
        request.method = method
        if (referer) {
            request.addHeader('referer', referer)
        }

        when:
        withFilters(action: "endpointMoved", controller: "api") {
            controller.endpointMoved()
        }

        then:
        response.status == status
        if (status == 200) {
            1 * controller.apiService.renderErrorFormat(*_) >> false
        }
        0 * controller.apiService._(*_)

        where:
        method | filterMethod | referer                        | status
        'GET'  | 'POST'       | 'http://hostname:4000/rundeck' | 200
        'GET'  | 'POST'       | 'http://wrong:4000/rundeck'    | 200
        'GET'  | 'POST'       | null                           | 200
        'GET'  | '*'          | 'http://hostname:4000/rundeck' | 200
        'GET'  | '*'          | 'http://wrong:4000/rundeck'    | 401
        'GET'  | '*'          | null                           | 401
        'POST' | '*'          | 'http://hostname:4000/rundeck' | 200
        'POST' | '*'          | 'http://wrong:4000/rundeck'    | 401
        'POST' | '*'          | null                           | 401
        'POST' | 'POST'       | 'http://hostname:4000/rundeck' | 200
        'POST' | 'POST'       | 'http://wrong:4000/rundeck'    | 401
        'POST' | 'POST'       | null                           | 401
    }

    @Unroll
    def "referer header filters https required #requireHttps ref #referer"() {
        given:
        controller.configurationService.grailsApplication = grailsApplication
        grailsApplication.config.clear()
        grailsApplication.config.rundeck.security.csrf.referer.filterMethod = '*'
        grailsApplication.config.rundeck.security.csrf.referer.allowApi = 'false'
        grailsApplication.config.rundeck.security.csrf.referer.requireHttps = requireHttps
        grailsApplication.config.grails.serverURL = serverurl
        controller.apiService = Mock(ApiService)
        params.api_version = "18"
        request.api_version = 18
        request.forwardURI = '/api/18/tokens'
        request.method = 'POST'
        if (referer) {
            request.addHeader('referer', referer)
        }

        when:
        withFilters(action: "apiTokenRemoveExpired", controller: "api") {
            controller.apiTokenRemoveExpired()
        }

        then:
        response.status == status
        controller.configurationService != null
        if (status == 200) {
            1 * controller.apiService.requireVersion(*_) >> false
        }
        0 * controller.apiService._(*_)

        where:
        requireHttps | serverurl                       | referer                                | status
        'true'       | 'https://hostname:4000/rundeck' | 'https://hostname:4000/rundeck'        | 200
        'true'       | 'https://hostname:4000/rundeck' | 'https://hostname:4000/rundeck/zinger' | 200
        'true'       | 'https://hostname:4000/rundeck' | 'http://hostname:4000/rundeck'         | 401
        'true'       | 'https://hostname:4000/rundeck' | 'http://wrong:4000/rundeck'            | 401
        'true'       | 'https://hostname:4000/rundeck' | 'https://wrong:4000/rundeck'           | 401
        'true'       | 'https://hostname:4000/rundeck' | null                                   | 401

        'false'      | 'https://hostname:4000/rundeck' | 'https://hostname:4000/rundeck'        | 200
        'false'      | 'https://hostname:4000/rundeck' | 'https://hostname:4000/rundeck/zinger' | 200
        'false'      | 'https://hostname:4000/rundeck' | 'http://hostname:4000/rundeck'         | 200
        'false'      | 'https://hostname:4000/rundeck' | 'http://wrong:4000/rundeck'            | 401
        'false'      | 'https://hostname:4000/rundeck' | 'https://wrong:4000/rundeck'           | 401
        'false'      | 'https://hostname:4000/rundeck' | null                                   | 401
    }
}
