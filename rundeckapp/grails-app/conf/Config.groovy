/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
grails.config.locations = System.properties["${appName}.config.locations"]?.split(",").collect{ "file:$it" }

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

        grails.profiler.disable=true
    }
    development{
        grails.serverURL="http://localhost:9090/rundeck"
        plugin.refreshDelay=5000
        grails.profiler.disable=false
    }
    test {
        grails.profiler.disable=true
    }
}
grails.json.legacy.builder = false
grails.mail.default.from="rundeck-server@localhost"
grails.converters.json.default.deep = true

grails.databinding.dateFormats = [
        //default grails patterns
        "yyyy-MM-dd HH:mm:ss.S",
        "yyyy-MM-dd'T'hh:mm:ss'Z'",

        // ISO8601 patterns
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ss.XXX",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
]

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
            debug 'org.apache.commons.httpclient'
            info 'grails.app.services.rundeck.services.ProjectManagerService'
            //off 'h2database'
            //info 'grails.app.utils.rundeck.codecs.SanitizedHTMLCodec'
        }
    }
}

environments{
    development{
        feature.incubator.'*'=true
        rundeck.feature.'*'.enabled=true
    }
    production{
        //disable feature toggling
        feature.incubator.feature = false
        //enable takeover schedule feature
        feature.incubator.jobs = true

        //enable dynamic workflow step descriptions in GUI by default
        rundeck.feature.workflowDynamicStepSummaryGUI.enabled = true
    }
}
beans {
    cacheManager {
        shared = true
    }
}
rundeck.metrics.enabled=true
rundeck.metrics.jmxEnabled=true
rundeck.metrics.requestFilterEnabled=true
rundeck.metrics.servletUrlPattern='/metrics/*'

grails.plugins.twitterbootstrap.fixtaglib = true

rundeck.execution.finalize.retryMax=10
rundeck.execution.finalize.retryDelay=5000
rundeck.execution.stats.retryMax=5
rundeck.execution.stats.retryDelay=5000
rundeck.gui.execution.tail.lines.default = 20
rundeck.gui.execution.tail.lines.max = 500

rundeck.execution.logs.fileStorage.cancelOnStorageFailure=true

rundeck.mail.template.subject='${notification.eventStatus} [${execution.project}] ${job.group}/${job.name} ${execution.argstring}'
rundeck.security.useHMacRequestTokens=true
rundeck.security.apiCookieAccess.enabled=true
rundeck.security.authorization.containerPrincipal.enabled=true
rundeck.security.authorization.container.enabled=true
rundeck.security.authorization.preauthenticated.enabled=false
rundeck.security.authorization.preauthenticated.attributeName=null
rundeck.security.authorization.preauthenticated.userNameHeader=null
rundeck.security.authorization.preauthenticated.userRolesHeader=null
rundeck.security.authorization.preauthenticated.delimiter=','

rundeck.web.metrics.servlets.metrics.enabled = true
rundeck.web.metrics.servlets.ping.enabled = true
rundeck.web.metrics.servlets.threads.enabled = true
rundeck.web.metrics.servlets.healthcheck.enabled = true

rundeck.gui.job.description.disableHTML=false
rundeck.pagination.default.max=20
rundeck.gui.clusterIdentityInHeader=false
rundeck.gui.clusterIdentityInFooter=true

rdeck.base='${userHome}/dev-3-svr/server/config'

rundeck.projectService.projectExportCache.spec= "expireAfterAccess=30m"
rundeck.projectManagerService.projectCache.spec='expireAfterAccess=10m,refreshAfterWrite=1m'

rundeck.logFileStorageService.startup.resumeMode = 'async'

rundeck.projectsStorageType='filesystem'

rundeck.ajax.compression='gzip'
rundeck.ajax.executionState.compression.nodeThreshold=500

rundeck.nodeService.nodeCache.spec='refreshInterval=30s'
rundeck.nodeService.nodeCache.enabled=true

grails.assets.less.compile = 'less4j'
grails.assets.plugin."twitter-bootstrap".excludes = ["**/*.less"]
grails.assets.plugin."twitter-bootstrap".includes = ["bootstrap.less"]

//turn off whitespace conversion to blank/null for data binding
grails.databinding.trimStrings=false

rundeck.executionMode='active'

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

plugin.platformCore.security.disabled=true
plugin.platformCore.navigation.disabled=true
plugin.platformCore.ui.disabled=true
plugin.platformCore.events.disabled=false
plugin.platformCore.events.gorm.disabled=true
plugin.platformCore.show.startup.info=false