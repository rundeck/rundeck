package rundeck.interceptors

import grails.testing.web.interceptor.InterceptorUnitTest
import jakarta.servlet.http.Cookie
import rundeck.services.feature.FeatureService
import spock.lang.Specification
import spock.lang.Unroll


class NextUiInterceptorSpec extends Specification implements InterceptorUnitTest<NextUiInterceptor> {

    def setup() {
        interceptor.featureService = Mock(FeatureService)
    }

    def cleanup() {
    }

    @Unroll
    void "Test nextUi interceptor matches #controller:#action"() {
        when: "A request matches the interceptor"
            withRequest(controller: controller, action: action)

        then: "The interceptor does match"
            interceptor.doesMatch() == shouldMatch

        where:
            controller           | action                  | shouldMatch
            'scheduledExecution' | 'update'                | true
            'scheduledExecution' | 'save'                  | true
            'scheduledExecution' | 'edit'                  | true
            'scheduledExecution' | 'create'                | true
            'scheduledExecution' | 'copy'                  | true
            'scheduledExecution' | 'createFromExecution'   | true
            'scheduledExecution' | 'show'                  | false
            'scheduledExecution' | 'delete'                | false
            'menu'               | 'jobs'                  | true
            'menu'               | 'home'                  | true
            'menu'               | 'index'                 | false
            'menu'               | 'projectHome'           | false
            'user'               | 'list'                  | true
            'user'               | 'edit'                  | false
            'other'              | 'someAction'            | false
    }

    void "Test nextUi interceptor sets nextUiCapable request attribute"() {
        given:
            interceptor.featureService = Mock(FeatureService) {
                featurePresent('nextUiMode') >> false
            }

        when:
            withRequest(controller: 'menu', action: 'jobs')
            boolean result = interceptor.before()

        then:
            result == true
            request.getAttribute('nextUiCapable') == true
    }

    @Unroll
    void "Test nextUi param logic: systemEnabled=#systemEnabled, cookie=#cookieValue -> nextUi=#expectedNextUi"() {
        given:
            interceptor.featureService = Mock(FeatureService) {
                featurePresent('nextUiMode') >> systemEnabled
            }
            if (cookieValue != null) {
                request.cookies = [new Cookie('nextUi', cookieValue)] as Cookie[]
            }

        when:
            withRequest(controller: 'menu', action: 'jobs')
            boolean result = interceptor.before()

        then:
            result == true
            request.getAttribute('nextUiCapable') == true
            if (expectedNextUi == null) {
                params.nextUi == null || !params.containsKey('nextUi')
            } else {
                params.nextUi == expectedNextUi
            }

        where:
            systemEnabled | cookieValue | expectedNextUi
            true          | 'false'     | false          // User explicitly disabled - respect preference
            true          | 'true'      | true           // User explicitly enabled
            true          | null        | true           // System enabled, no cookie - default to true
            false         | 'false'     | false          // User disabled
            false         | 'true'      | true           // User enabled
            false         | null        | null           // Nothing enabled - stays unset
    }

    void "Test cookie 'false' overrides system flag"() {
        given:
            interceptor.featureService = Mock(FeatureService) {
                featurePresent('nextUiMode') >> true
            }
            request.cookies = [new Cookie('nextUi', 'false')] as Cookie[]

        when:
            withRequest(controller: 'scheduledExecution', action: 'edit')
            boolean result = interceptor.before()

        then:
            result == true
            params.nextUi == false
    }

    void "Test system flag enables nextUi when no cookie set"() {
        given:
            interceptor.featureService = Mock(FeatureService) {
                featurePresent('nextUiMode') >> true
            }
            request.cookies = [] as Cookie[]

        when:
            withRequest(controller: 'menu', action: 'home')
            boolean result = interceptor.before()

        then:
            result == true
            params.nextUi == true
    }

    void "Test nextUi stays unset when system flag disabled and no cookie"() {
        given:
            interceptor.featureService = Mock(FeatureService) {
                featurePresent('nextUiMode') >> false
            }
            request.cookies = [] as Cookie[]

        when:
            withRequest(controller: 'scheduledExecution', action: 'create')
            boolean result = interceptor.before()

        then:
            result == true
            params.nextUi == null || !params.containsKey('nextUi')
    }

    /**
     * URL nextUi string values are normalized to boolean so templates can use plain truthiness.
     */
    @Unroll
    void "Test nextUi URL param '#urlValue' normalized to #expected when no cookie or flag"() {
        given:
            interceptor.featureService = Mock(FeatureService) {
                featurePresent('nextUiMode') >> false
            }

        when:
            if (urlValue != null) params.nextUi = urlValue
            withRequest(controller: 'scheduledExecution', action: 'create')
            interceptor.before()

        then:
            params.nextUi == expected
            params.containsKey('nextUi') == hasKey

        where:
            urlValue | expected | hasKey
            'true'   | true     | true
            'false'  | false    | true
            null     | null     | false
    }

    /**
     * The system flag takes precedence over a URL nextUi=false when no cookie is set.
     */
    void "Test system flag overrides URL nextUi=false when no cookie set"() {
        given:
            interceptor.featureService = Mock(FeatureService) {
                featurePresent('nextUiMode') >> true
            }

        when:
            params.nextUi = 'false'
            withRequest(controller: 'scheduledExecution', action: 'create')
            interceptor.before()

        then:
            params.nextUi == true
    }

    void "Test interceptor always returns true from before()"() {
        given:
            interceptor.featureService = Mock(FeatureService)

        when:
            withRequest(controller: 'menu', action: 'jobs')
            boolean result = interceptor.before()

        then:
            result == true
    }

    void "Test after() returns true"() {
        when:
            boolean result = interceptor.after()

        then:
            result == true
    }
}
