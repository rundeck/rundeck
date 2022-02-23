package rundeck.interceptors

import com.codahale.metrics.MetricRegistry
import com.dtolabs.rundeck.app.api.ApiMarshallerRegistrar
import com.dtolabs.rundeck.app.api.ApiVersions
import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.plugins.web.servlet.mvc.InvalidResponseHandler
import org.grails.plugins.web.servlet.mvc.ValidResponseHandler
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.grails.web.servlet.mvc.TokenResponseHandler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import rundeck.controllers.ProjectController
import rundeck.controllers.TokenVerifierController
import rundeck.services.ApiService
import spock.lang.Specification

class ApiVersionInterceptorSpec extends Specification implements InterceptorUnitTest<ApiVersionInterceptor> {

    def setup() {
        defineBeans{
            rundeckApiVersionSupplier(ApiVersions)
            apiMarshallerRegistrar(ApiMarshallerRegistrar)
        }
    }

    def cleanup() {

    }

    void "Test apiVersion interceptor matching"() {
        given:
        def service = Mock(ApiService)
        def msgSrc = Mock(MessageSource)
        defineBeans {
            metricRegistry(MetricRegistry)
            tokenVerifierController(TokenVerifierController)
            apiService(service)
            messageSource(msgSrc)
        }
        when:"A request matches the interceptor"
            withRequest(uri:"/api/apiVersion")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }

    void "Valid sync tokens allow api access"() {
        given:
        def service = Mock(ApiService)
        def msgSrc = Mock(MessageSource)
        def tkController = Mock(TokenVerifierController)

        defineBeans {
            metricRegistry(MetricRegistry)
            tokenVerifierController(tkController)
            apiService(service)
            messageSource(msgSrc)
        }

        ApiMarshallerRegistrar apiMarshallerRegistrar = new ApiMarshallerRegistrar()
        apiMarshallerRegistrar.registerApiMarshallers()
        when:
            params[SynchronizerTokensHolder.TOKEN_KEY] = "token"
            params[SynchronizerTokensHolder.TOKEN_URI] = "/"
            request.method = "POST"
            params.api_version = ApiVersions.API_CURRENT_VERSION.toString()
            interceptor.tokenVerifierController = tkController
            boolean allowed = interceptor.before()

        then:
            allowed

        and:
            1 * tkController.withForm({cls -> cls.call() }) >> new ValidResponseHandler()
            1 * tkController.refreshTokens()

    }

    void "Invalid sync tokens do not allow api access"() {
        given:
        def service = Mock(ApiService)
        def msgSrc = Mock(MessageSource)

        defineBeans {
            metricRegistry(MetricRegistry)
            tokenVerifierController(TokenVerifierController)
            apiService(service)
            messageSource(msgSrc)
        }

        ApiMarshallerRegistrar apiMarshallerRegistrar = new ApiMarshallerRegistrar()
        apiMarshallerRegistrar.registerApiMarshallers()
        when:
        request.remoteUser = "LoggedIn"
        session.api_access_allowed = false //This is set by ApiAccessInterceptor which fires before this interceptor
        params[SynchronizerTokensHolder.TOKEN_KEY] = "invalidtoken"
        params.api_version = ApiVersions.API_CURRENT_VERSION.toString()
        boolean allowed = interceptor.before()

        then:
        !allowed
        session.api_access_allowed == false

    }
}
