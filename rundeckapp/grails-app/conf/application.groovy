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

grails.controllers.upload.maxFileSize=26214400
grails.controllers.upload.maxRequestSize=26214400

environments {
    development{
        grails.serverURL="http://localhost:9090/rundeck"
        application.refreshDelay=5000
        grails.profiler.disable=false
        feature.incubator.'*'=true
        rundeck.feature.'enhanced-nodes'.enabled = true
        rundeck.feature.workflowDynamicStepSummaryGUI.enabled = true
        rundeck.feature.cleanExecutionsHistoryJob.enabled = true
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'create-drop','update'
            url = "jdbc:h2:file:./db/devDb"
        }

    }
    test {
        grails.profiler.disable=true
        dataSource {
            dbCreate = "create-drop"
            url = "jdbc:h2:file:./db/testDb"
        }
    }
    production {
//        grails.serverURL = "http://www.changeme.com"

        grails.profiler.disable=true
        //disable feature toggling
        feature.incubator.feature = false
        //enable takeover schedule feature
        feature.incubator.jobs = true

        rundeck.feature.'enhanced-nodes'.enabled = true
        rundeck.feature.'option-values-plugin'.enabled = true

        //enable dynamic workflow step descriptions in GUI by default
        rundeck.feature.workflowDynamicStepSummaryGUI.enabled = true
        rundeck.feature.cleanExecutionsHistoryJob.enabled = true
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:file:/rundeck/grailsh2"
            properties {
                jmxEnabled= true
                initialSize= 5
                maxActive= 50
                minIdle= 5
                maxIdle= 25
                maxWait= 10000
                maxAge= 600000
                timeBetweenEvictionRunsMillis= 5000
                minEvictableIdleTimeMillis= 60000
                validationQuery= "SELECT 1"
                validationQueryTimeout= 3
                validationInterval= 15000
                testOnBorrow= true
                testWhileIdle= true
                testOnReturn= false
                jdbcInterceptors= "ConnectionState"
                defaultTransactionIsolation= 2 // TRANSACTION_READ_COMMITTED
            }
        }
    }
}

grails.config.locations = [
        "classpath:QuartzConfig.groovy"
]

if(environment=="development"){
    grails.config.locations << "file:${userHome}/.grails/${appName}-config.properties"
}

grails.config.locations << "classpath:${appName}-config.properties"

grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"

grails.plugin.springsecurity.interceptUrlMap = [
        [pattern: '/j_security_check', access: ['permitAll']],
        [pattern: '/error',          access: ['permitAll']],
        [pattern: '/common/error',   access: ['permitAll']],
        [pattern: '/404',            access: ['permitAll']],
        [pattern: '/404.gsp',        access: ['permitAll']],
        [pattern: '/static/**',      access: ['permitAll']],
        [pattern: '/user-assets/**', access: ['permitAll']],
        [pattern: '/assets/**',      access: ['permitAll']],
        [pattern: '/favicon.ico',    access: ['permitAll']],
        [pattern: '/user/login',     access: ['permitAll']],
        [pattern: '/user/error',     access: ['permitAll']],
        [pattern: '/user/logout',    access: ['permitAll']],
        [pattern: '/user/loggedout', access: ['permitAll']],
        [pattern: '/feed/**',        access: ['permitAll']],
        [pattern: '/api/**',         access: ['permitAll']],
        [pattern: '/**',             access: ['IS_AUTHENTICATED_REMEMBERED']]
]

grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/user/login',     filters: 'none'],
        [pattern: '/error',          filters: 'none'],
        [pattern: '/user/error',     filters: 'none'],
        [pattern: '/common/error',   filters: 'none'],
        [pattern: '/static/**',      filters: 'none'],
        [pattern: '/user-assets/**', filters: 'none'],
        [pattern: '/assets/**',      filters: 'none'],
        [pattern: '/feed/**',        filters: 'none'],
        [pattern: '/api/**',         filters: 'JOINED_FILTERS'],
        [pattern: '/plugin/**',      filters: 'JOINED_FILTERS'],
        [pattern: '/404',            filters: 'none'],
        [pattern: '/404.gsp',        filters: 'none'],
        [pattern: '/favicon.ico',    filters: 'none'],
        [pattern: '/**',             filters: 'JOINED_FILTERS']
]
grails.plugin.springsecurity.useSecurityEventListener=true
grails.plugin.springsecurity.useHttpSessionEventPublisher=true
grails.plugin.springsecurity.apf.filterProcessesUrl = "/j_security_check"
grails.plugin.springsecurity.apf.usernameParameter = "j_username"
grails.plugin.springsecurity.apf.passwordParameter = "j_password"
grails.plugin.springsecurity.auth.loginFormUrl = "/user/login"
grails.plugin.springsecurity.logout.filterProcessesUrl = '/user/logout'
grails.plugin.springsecurity.logout.afterLogoutUrl = '/user/loggedout'
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = "/user/error"

grails.plugin.springsecurity.logout.handlerNames = [
        'rememberMeServices',
        'securityContextLogoutHandler',
        'userActionService']

grails.plugin.springsecurity.providerNames = [
        'anonymousAuthenticationProvider',
        'rememberMeAuthenticationProvider']

