package grails.securityheaders

import grails.plugins.*
import org.rundeck.grails.plugins.securityheaders.CSPSecurityHeaderProvider
import org.rundeck.grails.plugins.securityheaders.CustomSecurityHeaderProvider
import org.rundeck.grails.plugins.securityheaders.RundeckSecurityHeadersFilter
import org.rundeck.grails.plugins.securityheaders.XCTOSecurityHeaderProvider
import org.rundeck.grails.plugins.securityheaders.XFOSecurityHeaderProvider
import org.rundeck.grails.plugins.securityheaders.XXSSPSecurityHeaderProvider
import org.springframework.boot.web.servlet.FilterRegistrationBean

class GrailsSecurityheadersGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.3.8 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Grails Securityheaders" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
Brief summary/description of the plugin.
'''
    def profiles = ['web']
    def loadBefore = ['asset-pipeline']

    // URL to the plugin's documentation
    def documentation = "https://github.com/rundeck/rundeck"


    Closure doWithSpring() {
        { ->
            //can define custom headers
            customSecurityHeaderProvider(CustomSecurityHeaderProvider) {
                name = 'custom'
                defaultEnabled = false
            }
            /**
             * defines Content-Security-Policy
             */
            cspSecurityHeaderProvider(CSPSecurityHeaderProvider) {
                name = 'csp'
                defaultEnabled = false
            }
            /**
             * X-Content-Type-Options: nosniff
             */
            xctoSecurityHeaderProvider(XCTOSecurityHeaderProvider) {
                name = 'xcto'
                defaultEnabled = true
            }

            /**
             * X-XSS-Protection: 1
             */
            xxsspSecurityHeaderProvider(XXSSPSecurityHeaderProvider) {
                name = 'xxssp'
                defaultEnabled = true
            }

            /**
             * X-Frame-Options: deny
             */
            xfoSecurityHeaderProvider(XFOSecurityHeaderProvider) {
                name = 'xfo'
                defaultEnabled = true
            }
            rundeckSecurityHeadersFilter(RundeckSecurityHeadersFilter) {
                enabled = grailsApplication.config.rundeck?.security?.httpHeaders?.enabled in [true, 'true']
                config = grailsApplication.config.rundeck?.security?.httpHeaders?.provider
            }
            rundeckSecHeadersFilterReg(FilterRegistrationBean) {
                filter = ref("rundeckSecurityHeadersFilter")
                enabled = true
            }
        }
    }
}
