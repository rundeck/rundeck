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
    development {
        dataSource {
            dbCreate = "create-drop" // one of 'create', 'create-drop','update'
            url = "jdbc:h2:file:db/devDb"
        }
    }
    test {
        dataSource {
            dbCreate = "update"
            url = "jdbc:h2:file:./db/testDb"
        }
    }
    production {
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

grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"

grails.plugin.springsecurity.interceptUrlMap = [
        [pattern: '/user/j_security_check', access: ['permitAll']],
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
        [pattern: '/api/**',         filters: 'JOINED_FILTERS'],
        [pattern: '/404',            filters: 'none'],
        [pattern: '/404.gsp',        filters: 'none'],
        [pattern: '/**/js/**',       filters: 'none'],
        [pattern: '/**/css/**',      filters: 'none'],
        [pattern: '/**/images/**',   filters: 'none'],
        [pattern: '/**/favicon.ico', filters: 'none'],
        [pattern: '/**',             filters: 'JOINED_FILTERS']
]

grails.plugin.springsecurity.filterChain.filterNames = [
        'securityContextPersistenceFilter', 'logoutFilter',
        'rundeckPreauthFilter',
        'authenticationProcessingFilter', 'jaasApiIntegrationFilter',
        'securityContextHolderAwareRequestFilter',
        'rememberMeAuthenticationFilter', 'anonymousAuthenticationFilter',
        'exceptionTranslationFilter', 'filterInvocationInterceptor'
]

grails.plugin.springsecurity.apf.filterProcessesUrl = "/user/j_security_check"
grails.plugin.springsecurity.apf.usernameParameter = "j_username"
grails.plugin.springsecurity.apf.passwordParameter = "j_password"
grails.plugin.springsecurity.auth.loginFormUrl = "/user/login"
grails.plugin.springsecurity.logout.filterProcessesUrl = '/user/logout'
grails.plugin.springsecurity.logout.afterLogoutUrl = '/user/loggedout'
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = "/user/error"

grails.plugin.springsecurity.providerNames = [
        'preAuthenticatedAuthProvider',
        'jaasAuthProvider',
        'anonymousAuthenticationProvider',
        'rememberMeAuthenticationProvider']