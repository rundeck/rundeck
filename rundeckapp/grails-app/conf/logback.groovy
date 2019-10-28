import grails.util.BuildSettings
import grails.util.Environment
//import org.rundeck.util.logback.TrueConsoleAppender
//import org.springframework.boot.logging.logback.ColorConverter
//import org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter

import java.nio.charset.StandardCharsets

//conversionRule 'clr', ColorConverter
//conversionRule 'wex', WhitespaceThrowableProxyConverter

// See http://logback.qos.ch/manual/groovy.html for details on configuration

['org.codehaus.groovy.grails.web.servlet',  //  controllers
 'org.codehaus.groovy.grails.web.pages', //  GSP
 'org.codehaus.groovy.grails.web.sitemesh', //  layouts
 'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
 'org.codehaus.groovy.grails.web.mapping', // URL mapping
 'org.codehaus.groovy.grails.commons', // core / classloading
 'org.codehaus.groovy.grails.plugins', // plugins
 'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
 'org.springframework',
 'org.hibernate',
 'net.sf.ehcache.hibernate'].each {
    logger it, WARN, ['STDOUT'], false
}
['org.hibernate.cache.ehcache','org.springframework.beans.GenericTypeAwarePropertyDescriptor'].each {
    logger it, ERROR, ['STDOUT'], false
}

// Provides critical visibility into jaas config issues
['com.dtolabs.rundeck.jetty.jaas',
 'grails.plugin.springsecurity.web.authentication.GrailsUsernamePasswordAuthenticationFilter',
 'org.rundeck.jaas'].each {
    logger it, DEBUG, ['STDOUT'], false
}

logger "rundeckapp.BootStrap", INFO, ["STDOUT"], false
if (Environment.isDevelopmentMode()) {

    def targetDir = BuildSettings.TARGET_DIR
    if (targetDir != null) {
        appender("FULL_STACKTRACE", FileAppender) {
            file = "${targetDir}/stacktrace.log"
            append = true
            encoder(PatternLayoutEncoder) {
                charset = StandardCharsets.UTF_8
                pattern = "%level %logger - %msg%n"
            }
        }
    }

    logger 'rundeck.services.ProjectManagerService', INFO, ['STDOUT'], false
    logger 'org.rundeck.api.requests', INFO, ['STDOUT'], false
    logger 'org.rundeck.web.requests', INFO, ["STDOUT"], false
    logger 'org.rundeck.web.infosec', DEBUG, ["STDOUT"], false
    logger 'org.apache.commons.httpclient', DEBUG, ["STDOUT"], false
    logger 'rundeck.interceptors', DEBUG, ['STDOUT'], false
    logger("StackTrace", ERROR, ['FULL_STACKTRACE'], false)
    logger 'org.hibernate.orm.deprecation',ERROR,['STDOUT'],false

} else if(Environment.PRODUCTION == Environment.current) {

    logger 'org.mortbay.log', WARN, ['STDOUT'], false
    logger 'rundeck.interceptors', WARN, ['STDOUT'], false
    logger 'org.hibernate.orm.deprecation',ERROR,['STDOUT'],false

    //optional stacktrace log for production
    if (System.properties['rundeck.grails.stacktrace.enabled'] == 'true' &&
        System.properties['rundeck.grails.stacktrace.dir']) {

        String logDir = System.properties['rundeck.grails.stacktrace.dir']
        appender("FULL_STACKTRACE", RollingFileAppender) {
            append = true

            rollingPolicy(TimeBasedRollingPolicy) {
                FileNamePattern = "${logDir}/stacktrace-%d{yyyy-MM-dd}.zip"
            }
            encoder(PatternLayoutEncoder) {
                pattern = "%level %logger - %msg%n"
                pattern = "%d [%thread] %-5level %logger{36} %mdc - %msg%n"
            }
        }
    }
}
root(WARN, ['STDOUT'])
