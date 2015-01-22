// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

// if(System.properties["${appName}.config.location"]) {
//    grails.config.locations << "file:" + System.properties["${appName}.config.location"]
// }

grails.config.locations = [ ]

if(environment=="development"){
   grails.config.locations << "file:${userHome}/.grails/${appName}-config.properties"
}
if(System.properties["${appName}.config.location"]) {
   grails.config.locations << "file:" + System.properties["${appName}.config.location"]
}else{
    grails.config.locations << "classpath:${appName}-config.properties"
}

grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.types = [ // the first one is the default format
                      all:           '*/*', // 'all' maps to '*' or the first available format in withFormat
                      atom:          'application/atom+xml',
                      css:           'text/css',
                      csv:           'text/csv',
                      form:          'application/x-www-form-urlencoded',
                      html:          ['text/html','application/xhtml+xml'],
                      js:            'text/javascript',
                      json:          ['application/json', 'text/json'],
                      multipartForm: 'multipart/form-data',
                      rss:           'application/rss+xml',
                      text:          'text/plain',
                      hal:           ['application/hal+json','application/hal+xml'],
                      xml:           ['text/xml', 'application/xml'],
                      yaml:          ['text/yaml', 'application/yaml']
]
grails.mime.use.accept.header = true
// The default codec used to encode data with ${}
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
grails.converters.encoding="UTF-8"

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

// set per-environment serverURL stem for creating absolute links
environments {
    production {
//        grails.serverURL = "http://www.changeme.com"
    }
    development{
        grails.serverURL="http://localhost:9090/rundeck"
        plugin.refreshDelay=5000
    }
    test {
    }
}
grails.json.legacy.builder = false
grails.mail.default.from="rundeck-server@localhost"

// log4j configuration
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
        }
    }
}

environments{
    development{
        feature.incubator.'*'=true
    }
    production{
        //disable feature toggling
        feature.incubator.feature = false
        //enable takeover schedule feature
        feature.incubator.jobs = true
    }
}

rundeck.metrics.enabled=true
rundeck.metrics.jmxEnabled=true
rundeck.metrics.requestFilterEnabled=true
rundeck.metrics.servletUrlPattern='/metrics/*'

grails.plugins.twitterbootstrap.fixtaglib = true

rundeck.execution.finalize.retryMax=10
rundeck.execution.finalize.retryDelay=5000
rundeck.execution.stats.retryMax=3
rundeck.execution.stats.retryDelay=5000
rundeck.gui.execution.tail.lines.default = 20
rundeck.gui.execution.tail.lines.max = 500

rundeck.mail.template.subject='${notification.eventStatus} [${execution.project}] ${job.group}/${job.name} ${execution.argstring}'
rundeck.security.useHMacRequestTokens=true
rundeck.security.apiCookieAccess.enabled=true

rundeck.web.metrics.servlets.metrics.enabled = true
rundeck.web.metrics.servlets.ping.enabled = true
rundeck.web.metrics.servlets.threads.enabled = true
rundeck.web.metrics.servlets.healthcheck.enabled = true

rundeck.gui.job.description.disableHTML=false

grails.assets.less.compile = 'less4j'
grails.assets.plugin."twitter-bootstrap".excludes = ["**/*.less"]
grails.assets.plugin."twitter-bootstrap".includes = ["bootstrap.less"]

//turn off whitespace conversion to blank/null for data binding
grails.databinding.trimStrings=false

// GSP settings
grails {
    views {
        gsp {
            encoding = 'UTF-8'
            htmlcodec = 'xml' // use xml escaping instead of HTML4 escaping
            codecs {
                expression = 'html' // escapes values inside null
                scriptlet = 'html' // escapes output from scriptlets in GSPs
                taglib = 'none' // escapes output from taglibs
                staticparts = 'none' // escapes output from static template parts
            }
        }
        // escapes all not-encoded output at final stage of outputting
        filteringCodecForContentType {
            //'text/html' = 'html'
        }
    }
}

