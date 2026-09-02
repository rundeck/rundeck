
hibernate {
    cache.queries = true
    cache.use_second_level_cache = true
    cache.use_query_cache = true
    cache.region.factory_class = "jcache"
    cache.ehcache.missing_cache_strategy = "create"
    javax{
        cache{
            provider='org.ehcache.jsr107.EhcacheCachingProvider'
            missing_cache_strategy = "create"
            uri="jcache.xml"
        }
    }
}

dataSource {
    pooled = true
    jmxExport = true
    // Same reason as the url in the environment blocks below: Grails 8 reads only this nested map,
    // not the flat dataSource.* keys from rundeck-config.properties. Supplying the url alone is not
    // enough -- it would be opened with the H2 driver, which fails with "Access to
    // DialectResolutionInfo cannot be null" against MySQL or PostgreSQL. The literals stay as the
    // defaults for an H2 deployment that configures nothing.
    driverClassName= rundeckapp.init.DefaultRundeckConfigPropertyLoader.configuredDataSourceSetting('driverClassName') ?:
            "org.h2.Driver"
    username = rundeckapp.init.DefaultRundeckConfigPropertyLoader.configuredDataSourceSetting('username') ?:
            "sa"
    password = rundeckapp.init.DefaultRundeckConfigPropertyLoader.configuredDataSourceSetting('password') ?:
            ''
}

grails.controllers.upload.maxFileSize=26214400
grails.controllers.upload.maxRequestSize=26214400

grails.plugin.databasemigration.changelog = "changelog.groovy"



environments {
    development{
        grails.serverURL="http://localhost:9090/rundeck"
        application.refreshDelay=5000
        grails.profiler.disable=false
        rundeck.execution.logs.fileStorage.generateExecutionXml=true
        feature.incubator.'*'=true

        rundeck.scm.startup.initDeferred=false

        dataSource {
            dbCreate = "none" // one of 'create', 'create-drop','update'
            // NON_KEYWORDS is mandatory: ScheduledExecution has columns named MINUTE, HOUR, MONTH,
            // YEAR and SECONDS, all reserved in H2, and Hibernate emits them unquoted. Without it
            // every job-listing query dies with "Syntax error ... expected identifier".
            // The `test` block below already had this; `development` did not.
            // rundeck-config.properties is the only file a deployment can edit, and on Grails 8
            // the flat dataSource.url key it publishes is not consulted when the framework builds
            // the bean -- only this nested map is. Asking for the value here puts the operator's
            // choice where it is actually read; the literal below stays as the fallback for when
            // no rundeck-config supplies one.
            url = rundeckapp.init.DefaultRundeckConfigPropertyLoader.configuredDataSourceUrl() ?:
                    "jdbc:h2:file:./db/devDb;NON_KEYWORDS=MONTH,HOUR,MINUTE,YEAR,SECONDS"
        }
        grails.plugin.databasemigration.updateOnStart=true

        spring.h2.console.enabled=true

        //enable greenmail plugin in build.gradle, and set this value in dev mode
        //grails.mail.port = com.icegreen.greenmail.util.ServerSetupTest.SMTP.port
    }
    test {
        def rdeckbasedir = File.createTempDir()
        rdeckbasedir.deleteOnExit()
        System.setProperty("rdeck.base",rdeckbasedir.absolutePath)
        grails.profiler.disable=true
        dataSource {
            dbCreate = "none"
            // rundeck-config.properties is the only file a deployment can edit, and on Grails 8
            // the flat dataSource.url key it publishes is not consulted when the framework builds
            // the bean -- only this nested map is. Asking for the value here puts the operator's
            // choice where it is actually read; the literal below stays as the fallback for when
            // no rundeck-config supplies one.
            url = rundeckapp.init.DefaultRundeckConfigPropertyLoader.configuredDataSourceUrl() ?:
                    "jdbc:h2:file:./db/testDb;NON_KEYWORDS=MONTH,HOUR,MINUTE,YEAR,SECONDS"
        }
        grails.plugin.databasemigration.updateOnStart=true

    }
    production {
//        grails.serverURL = "http://www.changeme.com"

        grails.profiler.disable=true
        //disable feature toggling
        feature.incubator.feature = false
        //enable takeover schedule feature
        feature.incubator.jobs = true

        rundeck.execution.logs.fileStorage.generateExecutionXml=true

        grails.plugin.databasemigration.updateOnStart=true

        dataSource {
            dbCreate = "none"
            //NON_KEYWORDS required -- see the development block above
            // rundeck-config.properties is the only file a deployment can edit, and on Grails 8
            // the flat dataSource.url key it publishes is not consulted when the framework builds
            // the bean -- only this nested map is. Asking for the value here puts the operator's
            // choice where it is actually read; the literal below stays as the fallback for when
            // no rundeck-config supplies one.
            url = rundeckapp.init.DefaultRundeckConfigPropertyLoader.configuredDataSourceUrl() ?:
                    "jdbc:h2:file:/rundeck/grailsh2;NON_KEYWORDS=MONTH,HOUR,MINUTE,YEAR,SECONDS"
        }
    }
}

grails.config.locations = [
        "classpath:QuartzConfig.groovy"
]

grails.plugin.springsecurity.securityConfigType = "InterceptUrlMap"

// Grails 7: Order matters! More specific patterns must come before catch-all /**
grails.plugin.springsecurity.interceptUrlMap = [
        [pattern: '/assets/**',      access: ['permitAll']], // FIRST: Asset pipeline must be before /**
        [pattern: '/static/**',      access: ['permitAll']],
        [pattern: '/user-assets/**', access: ['permitAll']],
        [pattern: '/j_security_check', access: ['permitAll']],
        [pattern: '/error/**',        access: ['permitAll']],
        [pattern: '/common/error',   access: ['permitAll']],
        [pattern: '/404',            access: ['permitAll']],
        [pattern: '/404.gsp',        access: ['permitAll']],
        [pattern: '/favicon.ico',    access: ['permitAll']],
        [pattern: '/user/login',     access: ['permitAll']],
        [pattern: '/user/reset',     access: ['permitAll']],
        [pattern: '/login/oauth2/**',access: ['permitAll']],
        [pattern: '/user/error',     access: ['permitAll']],
        [pattern: '/user/logout',    access: ['permitAll']],
        [pattern: '/user/loggedout', access: ['permitAll']],
        [pattern: '/feed/**',        access: ['permitAll']],
        [pattern: '/svc/api/**',     access: ['permitAll']],
        [pattern: '/api/**',         access: ['permitAll']],
        [pattern: '/metrics/**',     access: ['permitAll']], // Legacy Dropwizard metrics endpoints
        [pattern: '/health',         access: ['permitAll']],
        [pattern: '/actuator/**',    access: ['permitAll']],
        [pattern: '/actuator/health/**',    access: ['permitAll']],
        [pattern: '/monitoring/**',  access: ['permitAll']], // Spring Boot Actuator endpoints
        [pattern: '/.well-known/**', access: ['permitAll']],
        [pattern: '/**',             access: ['IS_AUTHENTICATED_REMEMBERED']] // LAST: Catch-all
]

grails.plugin.springsecurity.filterChain.chainMap = [
        [pattern: '/user/login',     filters: 'none'],
        [pattern: '/user/reset',     filters: 'none'],
        [pattern: '/j_security_check', filters: 'JOINED_FILTERS,-csrf'], // Form login endpoint - needs auth filters
        [pattern: '/error/**',       filters: 'JOINED_FILTERS,-csrf'],
        [pattern: '/user/error',     filters: 'none'],
        [pattern: '/common/error',   filters: 'none'],
        [pattern: '/static/**',      filters: 'none'],
        [pattern: '/user-assets/**', filters: 'none'],
        [pattern: '/assets/**',      filters: 'none'],
        [pattern: '/feed/**',        filters: 'none'],
        [pattern: '/svc/api/**',     filters: 'JOINED_FILTERS,-csrf'],
        [pattern: '/api/**',         filters: 'JOINED_FILTERS,-csrf'],
        [pattern: '/metrics/**',     filters: 'none'], // Legacy Dropwizard metrics endpoints
        [pattern: '/plugin/**',      filters: 'JOINED_FILTERS,-csrf'],
        [pattern: '/404',            filters: 'none'],
        [pattern: '/404.gsp',        filters: 'none'],
        [pattern: '/favicon.ico',    filters: 'none'],
        [pattern: '/health',         filters: 'none'],
        [pattern: '/actuator/**',    filters: 'none'],
        [pattern: '/actuator/health/**',    filters: 'none'],
        [pattern: '/monitoring/**',  filters: 'JOINED_FILTERS,-csrf'], // Allow filters to run (for config check)
        [pattern: '/.well-known/**', filters: 'none'],
        [pattern: '/**',             filters: 'JOINED_FILTERS,-csrf']
]

// Grails 7/Spring Security 6: Disable Spring Security's CSRF filter
// Rundeck uses its own HMac-based CSRF protection (rundeck.security.useHMacRequestTokens)
grails.plugin.springsecurity.csrf.enabled = false
grails.plugin.springsecurity.printStatusMessages=false
grails.plugin.springsecurity.useSecurityEventListener=true
grails.plugin.springsecurity.useHttpSessionEventPublisher=true
grails.plugin.springsecurity.apf.filterProcessesUrl = "/j_security_check"
grails.plugin.springsecurity.apf.usernameParameter = "j_username"
grails.plugin.springsecurity.apf.passwordParameter = "j_password"
grails.plugin.springsecurity.auth.loginFormUrl = "/user/login"
grails.plugin.springsecurity.logout.filterProcessesUrl = '/user/logout'
grails.plugin.springsecurity.logout.afterLogoutUrl = '/user/loggedout'
grails.plugin.springsecurity.failureHandler.defaultFailureUrl = "/user/error"
// Grails 7/Spring Security 6: Prevent Chrome DevTools protocol URLs from being used as redirects
grails.plugin.springsecurity.successHandler.defaultTargetUrl = '/menu/home'
grails.plugin.springsecurity.successHandler.alwaysUseDefault = false
grails.plugin.springsecurity.successHandler.useReferer = false
grails.plugin.springsecurity.ajaxHeader = 'AJAX AUTH DISABLED\u0000'
grails.plugin.springsecurity.logout.handlerNames = [
        'rememberMeServices',
        'securityContextLogoutHandler',
        'userActionService',
        'auditEventsService']


grails.plugin.springsecurity.providerNames = [
        'anonymousAuthenticationProvider',
        'rememberMeAuthenticationProvider']
