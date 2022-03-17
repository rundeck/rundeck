package rundeck.interceptors

import com.codahale.metrics.MetricRegistry
import com.codahale.metrics.Timer
import com.dtolabs.rundeck.app.api.ApiMarshallerRegistrar
import com.dtolabs.rundeck.app.api.ApiVersions
import grails.testing.web.interceptor.InterceptorUnitTest
import org.grails.plugins.web.servlet.mvc.ValidResponseHandler
import org.grails.spring.beans.factory.InstanceFactoryBean
import org.grails.web.converters.configuration.ConvertersConfigurationHolder
import org.grails.web.servlet.mvc.SynchronizerTokensHolder
import org.springframework.context.MessageSource
import rundeck.controllers.TokenVerifierController
import rundeck.services.ApiService
import spock.lang.Specification

class ApiVersionInterceptorSpec extends Specification implements InterceptorUnitTest<ApiVersionInterceptor> {

    def setup() {
        def mockMetric = Mock(MetricRegistry){
            _ * timer(_)>>Mock(Timer){

            }
        }
        def mockToken = Mock(TokenVerifierController)
        def apiServiceMock = Mock(ApiService)
        def msgSrc = Mock(MessageSource)
        defineBeans {
            metricRegistry(InstanceFactoryBean,mockMetric)
            tokenVerifierController(InstanceFactoryBean,mockToken)
            apiService(InstanceFactoryBean, apiServiceMock)
            delegate.'messageSource'(InstanceFactoryBean, msgSrc)
        }
    }

    def cleanup() {

    }

    void "Test apiVersion interceptor matching"() {
        when:"A request matches the interceptor"
            withRequest(uri:"/api/apiVersion")

        then:"The interceptor does match"
            interceptor.doesMatch()
    }

    void "Valid sync tokens allow api access"() {
        given:
        def tkController = Mock(TokenVerifierController)
        interceptor.tokenVerifierController = tkController


        ApiMarshallerRegistrar apiMarshallerRegistrar = new ApiMarshallerRegistrar()
        apiMarshallerRegistrar.registerApiMarshallers()
        when:
            params[SynchronizerTokensHolder.TOKEN_KEY] = "token"
            params[SynchronizerTokensHolder.TOKEN_URI] = "/"
            request.method = "POST"
            params.api_version = ApiVersions.API_CURRENT_VERSION.toString()
            boolean allowed = interceptor.before()

        then:
            allowed

        and:
            1 * tkController.withForm({cls -> cls.call() }) >> new ValidResponseHandler()
            1 * tkController.refreshTokens()

    }

    void "api access before marshallers registered causes 503"() {
        given:
            ConvertersConfigurationHolder.clear()

        when:
            request.method = "GET"
            params.api_version = ApiVersions.API_CURRENT_VERSION.toString()
            boolean allowed = interceptor.before()

        then:
            allowed
            request.apiVersionStatusNotReady

    }

    void "Invalid sync tokens do not allow api access"() {
        given:

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
