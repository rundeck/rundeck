import org.codehaus.groovy.grails.commons.GrailsApplication

class RefererFilters {
    // referer must match serverURL, optionally https
    def grailsApplication

    def filters = {
        checkReferer(controller: '*', action: '*') {
            before = {
                def csrf = grailsApplication.config.rundeck.security.csrf
                if(csrf && csrf != 'NONE'){
                    println(csrf)
                    //grailsApplication.config.rundeck.clusterMode.enabled
                    if(csrf == 'POST') {
                        if (request.method.toUpperCase() == "POST") {
                            println('filter post')
                            def validRefererPrefix = "^${grailsApplication.config.grails.serverURL}".replace("http", "https?")
                            def referer = request.getHeader('Referer')
                            return referer && referer =~ validRefererPrefix
                        }
                    }else if(csrf == '*'){
                        println('filter anything')
                        def validRefererPrefix = "^${grailsApplication.config.grails.serverURL}".replace("http", "https?")
                        def referer = request.getHeader('Referer')
                        if(request.method.toUpperCase() == 'GET' && (referer == null || referer.endsWith("user/login"))) {
                            //excluding the user/login screen
                            println('return true')
                            return true
                        }else{
                            return referer && referer =~ validRefererPrefix
                        }
                    }
                }

            }
        }
    }
}