package rundeck.interceptors

import org.grails.plugins.testing.GrailsMockHttpServletRequest
import rundeck.services.ConfigurationService
import spock.lang.Specification
import spock.lang.Unroll

class DefaultInterceptorHelperSpec extends Specification {

    @Unroll
    def "MatchesAllowedAsset"() {
        given:
        DefaultInterceptorHelper interceptorHelper = new DefaultInterceptorHelper()
        interceptorHelper.configurationService = Mock(ConfigurationService) {
            getValue("security.interceptor.allowed.controllers",[]) >> controllerList
            getValue("security.interceptor.allowed.paths",[]) >> pathList
            getBoolean("gui.staticUserResources.enabled", false) >> statusUserResEnabled
        }
        def rq = new GrailsMockHttpServletRequest()
        rq.setPathInfo(pathInfo)

        when:
        interceptorHelper.afterPropertiesSet()
        def actual = interceptorHelper.matchesAllowedAsset(controllerName, rq)


        then:
        expected == actual

        where:
        expected | controllerName | pathInfo                     | statusUserResEnabled  | controllerList               | pathList
        false    | "user"         | "/user/123"                  | false                 | ["static", "user-assets"]    | ["/error","/favicon.ico","/health"]
        false    | "project"      | "/user/proj1"                | false                 | ["static", "user-assets"]    | ["/error","/favicon.ico","/health"]
        true     | null           | "/favicon.ico"               | false                 | ["static", "user-assets"]    | ["/error","/favicon.ico","/health"]
        true     | "user-assets"  | "/user-assets/fav-icon.png"  | false                 | ["static", "user-assets"]    | ["/error","/favicon.ico","/health"]
        false    | null           | "/user-assets/sub1/res.png"  | false                 | ["static", "user-assets"]    | ["/error","/favicon.ico","/health"]
        true     | null           | "/user-assets/sub1/res.png"  | true                  | ["static", "user-assets"]    | ["/error","/favicon.ico","/health"]
    }

}
