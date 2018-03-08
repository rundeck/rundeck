hibernate {
    cache.queries = true
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.provider_class = "net.sf.ehcache.hibernate.EhCacheProvider"
    cache.region.factory_class = "org.hibernate.cache.ehcache.EhCacheRegionFactory"
}
dataSource {
    pooled = true
    jmxExport = true
    driverClassName= "org.h2.Driver"
    username = "sa"
    password = ''
}

environments {
    production {
//        grails.serverURL = "http://www.changeme.com"

        grails.profiler.disable=true
        //disable feature toggling
        feature.incubator.feature = false
        //enable takeover schedule feature
        feature.incubator.jobs = true

        //enable dynamic workflow step descriptions in GUI by default
        rundeck.feature.workflowDynamicStepSummaryGUI.enabled = true
    }
    development{
        grails.serverURL="http://localhost:9090/rundeck"
        plugin.refreshDelay=5000
        grails.profiler.disable=false
        feature.incubator.'*'=true
        rundeck.feature.'*'.enabled=true
    }
    test {
        grails.profiler.disable=true
    }
}

grails.config.locations = [ ]

if(environment=="development"){
    grails.config.locations << "file:${userHome}/.grails/${appName}-config.properties"
}
if(System.properties["${appName}.config.location"]) {
    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
}else{
    grails.config.locations << "classpath:${appName}-config.properties"
}

grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"

grails.plugin.springsecurity.interceptUrlMap = [
        [pattern: '/j_security_check', access: ['permitAll']],
        [pattern: '/error',          access: ['permitAll']],
        [pattern: '/error.gsp',      access: ['permitAll']],
        [pattern: '/404',            access: ['permitAll']],
        [pattern: '/404.gsp',        access: ['permitAll']],
        [pattern: '/static/**',      access: ['permitAll']],
        [pattern: '/assets/**',      access: ['permitAll']],
        [pattern: '/**/js/**',       access: ['permitAll']],
        [pattern: '/**/css/**',      access: ['permitAll']],
        [pattern: '/**/images/**',   access: ['permitAll']],
        [pattern: '/**/favicon.ico', access: ['permitAll']],
        [pattern: '/user/login',     access: ['permitAll']],
        [pattern: '/user/error',     access: ['permitAll']],
        [pattern: '/user/logout',    access: ['permitAll']],
        [pattern: '/user/loggedout', access: ['permitAll']],
        [pattern: '/feed/**',        access: ['permitAll']],
        [pattern: '/api/**',         access: ['permitAll']],
        [pattern: '/test/**',        access: ['permitAll']],
        [pattern: '/**',             access: ['IS_AUTHENTICATED_REMEMBERED']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/static/**',      filters: 'none'],
        [pattern: '/assets/**',      filters: 'none'],
        [pattern: '/feed/**',        filters: 'none'],
        [pattern: '/test/**',        filters: 'none'],
        [pattern: '/api/**',         filters: 'none'],
        [pattern: '/404',            filters: 'none'],
        [pattern: '/404.gsp',        filters: 'none'],
        [pattern: '/**/js/**',       filters: 'none'],
        [pattern: '/**/css/**',      filters: 'none'],
        [pattern: '/**/images/**',   filters: 'none'],
        [pattern: '/**/favicon.ico', filters: 'none'],
        [pattern: '/**',             filters: 'JOINED_FILTERS']
]

grails.plugin.springsecurity.filterChain.filterNames = [
        'authFilter','securityContextPersistenceFilter', 'logoutFilter',
        'authenticationProcessingFilter', 'jaasApiIntegrationFilter',
        'securityContextHolderAwareRequestFilter',
        'rememberMeAuthenticationFilter', 'anonymousAuthenticationFilter',
        'exceptionTranslationFilter', 'filterInvocationInterceptor'
]

grails.plugin.springsecurity.apf.filterProcessesUrl = "/j_security_check"
grails.plugin.springsecurity.apf.usernameParameter = "j_username"
grails.plugin.springsecurity.apf.passwordParameter = "j_password"
grails.plugin.springsecurity.auth.loginFormUrl = "/user/login"
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = "/user/error"

grails.plugin.springsecurity.providerNames = [
        'jaasAuthProvider',
        'anonymousAuthenticationProvider',
        'rememberMeAuthenticationProvider']

log4j={
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}
    root {
        error()
        additivity = true
    }


    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'

    warn 'org.mortbay.log'
    warn 'grails.app.filters.AuthorizationFilters'
    info 'grails.app.conf'
//    info 'com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization'

    appenders {
        environments {
            development {
                console name: "access", layout: pattern(conversionPattern: "[%d{ISO8601}] \"%X{method} %X{uri}\" %X{duration} %X{remoteHost} %X{secure} %X{remoteUser} %X{authToken} %X{project} [%X{contentType}] (%X{userAgent})%n")
            }
        }
        if (System.properties['rundeck.grails.stacktrace.enabled']=='true'
                && System.properties['rundeck.grails.stacktrace.dir']) {
            String logDir = System.properties['rundeck.grails.stacktrace.dir']
            rollingFile name: 'stacktrace',
                    maximumFileSize: 10 * 1024 * 1024,
                    file: "$logDir/stacktrace.log",
                    layout: pattern(conversionPattern: '%d [%t] %-5p %c{2} %x - %m%n'),
                    maxBackupIndex: 10
        } else {
            delegate.'null'( name: 'stacktrace')
        }
    }
    environments {
        development {
            info 'org.rundeck.api.requests'
//            info 'org.rundeck.web.requests'
//            debug 'org.rundeck.web.infosec'
            debug 'org.apache.commons.httpclient'
            info 'grails.app.services.rundeck.services.ProjectManagerService'
            //off 'h2database'
            //info 'grails.app.utils.rundeck.codecs.SanitizedHTMLCodec'
        }
    }
}