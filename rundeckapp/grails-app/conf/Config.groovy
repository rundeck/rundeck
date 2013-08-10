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
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      yaml: ['text/yaml', 'application/yaml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
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
        rundeck.v14.rdbsupport = "true"
    }
    test {
        rundeck.v14.rdbsupport = "true"
    }
}

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
//    info 'com.dtolabs.rundeck.core.authorization.providers.SAREAuthorization'

    appenders {
        environments {
            development {
                console name: "access", layout: pattern(conversionPattern: "[%d{ISO8601}] \"%X{method} %X{uri}\" %X{duration} %X{remoteHost} %X{secure} %X{remoteUser} %X{authToken} %X{project} [%X{contentType}] (%X{userAgent})%n")
            }
        }
    }
    environments {
        development {
            info 'org.rundeck.api.requests'
            info 'org.rundeck.web.requests'
        }
    }
}


