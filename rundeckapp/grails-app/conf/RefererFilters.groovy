class RefererFilters {
    def grailsApplication

    def filters = {
        checkReferer(controller: 'user', action: 'login',invert: true) {
            before = {
                def csrf = grailsApplication.config.rundeck.security.csrf
                if(csrf && csrf != 'NONE'){
                    if(csrf == 'POST') {
                        if (request.method.toUpperCase() == "POST") {
                            // referer must match serverURL, optionally https
                            def validRefererPrefix = "^${grailsApplication.config.grails.serverURL}".replace("http", "https?")
                            def referer = request.getHeader('Referer')
                            return referer && referer =~ validRefererPrefix
                        }
                    }else if(csrf == '*'){
                        def validRefererPrefix = "^${grailsApplication.config.grails.serverURL}".replace("http", "https?")
                        def referer = request.getHeader('Referer')
                        return referer && referer =~ validRefererPrefix

                    }
                }

            }
        }
    }
}