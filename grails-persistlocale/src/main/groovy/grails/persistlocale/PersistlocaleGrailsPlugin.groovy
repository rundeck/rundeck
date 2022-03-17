package grails.persistlocale

import grails.plugins.*
import org.springframework.web.servlet.i18n.CookieLocaleResolver

class PersistlocaleGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "4.0.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def title = "Persistlocale" // Headline display name of the plugin
    def author = "Your name"
    def authorEmail = ""
    def description = '''\
use Cookie based resolver for Locale.
'''
    def profiles = ['web']
    def loadAfter = ['i18n']

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/grails-persistlocale"

    Closure doWithSpring() {
        { ->
            localeResolver(CookieLocaleResolver) {
                cookieMaxAge = grailsApplication.config.getProperty("rundeck.web.cookie.localeCookieExpiration",Integer.class, 7776000) //90days
                cookieName = grailsApplication.config.getProperty("rundeck.web.cookie.localeCookieName",String.class, 'rundeck.LOCALE')
            }
        }
    }
}
