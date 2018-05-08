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

/*
 * The following allows grails to leverage a different url setting for maven central. This would
 * typically be passed along as a -D parameter to grails, ie: grails -Dmaven.central.url=http://...
 */
def mavenCentralUrl = 'http://repo1.maven.org/maven2/'
if (System.properties['maven.central.url']) {
    mavenCentralUrl = System.properties['maven.central.url']
}
println "Maven Central: ${mavenCentralUrl}"


grails.project.target.level = 1.8
grails.project.source.level = 1.8

grails.project.dependency.resolver = "maven"

Boolean mavenCredsDefined = false
def mavenRealm
def mavenHost
def mavenUser
def mavenPassword

if (System.properties['maven.realm'] && System.properties['maven.host'] && System.properties['maven.user'] && System.properties['maven.password']) {
    mavenCredsDefined = true
    mavenRealm = System.properties['maven.realm']
    mavenHost = System.properties['maven.host']
    mavenUser = System.properties['maven.user']
    mavenPassword = System.properties['maven.password']

    println "Maven Credentials:\n\tRealm: ${mavenRealm}\n\tHost: ${mavenHost}\n\tUser: ${mavenUser}"
}

def grailsCentralUrl = 'http://grails.org/plugins'
if (System.properties['grails.central.url']) {
    grailsCentralUrl = System.properties['grails.central.url']
}
if(System.properties['disable.grails.central']) {
    println 'Grails Central: DISABLED'
} else {
    println "Grails Central: ${grailsCentralUrl}"
}

grails.plugin.location.webrealms = 'webrealms'
grails.plugin.location.metricsweb = 'metricsweb'
grails.plugin.location.authfilter = 'authfilter'
grails.servlet.version = "3.0"
grails.project.dependency.resolution = {
    inherits 'global' // inherit Grails' default dependencies
    log 'warn' // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        inherits false

        grailsPlugins()
        grailsHome()
        mavenLocal()
        mavenRepo mavenCentralUrl
        if(!System.properties['disable.grails.central']) {
            grailsCentral()
//            grailsRepo grailsCentralUrl
        }
    }

    if (mavenCredsDefined) {
        credentials {
            realm = mavenRealm
            host = mavenHost
            username = mavenUser
            password = mavenPassword
        }
    }


    rundeckVersion = System.getProperty("RUNDECK_VERSION", appVersion)
    println "Application Version: ${rundeckVersion}"

    plugins {
        compile (':less-asset-pipeline:2.7.2')
        compile ':twitter-bootstrap:3.3.2.1'
        compile (':asset-pipeline:2.7.2')
        compile ':cache:1.1.8'
        compile ":platform-core:1.0.0"
        runtime (':hibernate5:5.1.5')
        runtime ':mail:1.0.7', ':quartz:1.0.2', ':executor:0.3'

        runtime ':profiler:0.5'
        runtime ':miniprofiler:0.4.1'
        provided ':codenarc:0.22'
        build   ':jetty:3.0.0'
    }

    dependencies {

        build 'org.yaml:snakeyaml:1.9'
        compile 'org.yaml:snakeyaml:1.9',
                'org.apache.ant:ant:1.8.3',
                'org.apache.ant:ant-jsch:1.8.3',
                'com.jcraft:jsch:0.1.54',
                'log4j:log4j:1.2.17',
                'commons-collections:commons-collections:3.2.2',
                'commons-codec:commons-codec:1.5',
                'com.fasterxml.jackson.core:jackson-core:2.8.11',
                'com.fasterxml.jackson.core:jackson-databind:2.8.11.1',
                'com.fasterxml.jackson.core:jackson-annotations:2.8.11',
                'com.codahale.metrics:metrics-core:3.0.1',
                'com.google.guava:guava:24.1.1-jre',
                'org.owasp.encoder:encoder:1.2',
                'org.quartz-scheduler:quartz:2.2.1',
                'com.atlassian.commonmark:commonmark:0.9.0',
                'com.atlassian.commonmark:commonmark-ext-gfm-tables:0.9.0',
                'com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20160614.1'

        compile("org.rundeck:rundeck-core:${rundeckVersion}")
        compile("org.rundeck:rundeck-storage-filesys:${rundeckVersion}")

        runtime 'org.postgresql:postgresql:42.0.0'
        runtime 'mysql:mysql-connector-java:5.1.35'

        //BEGIN fix hibernate4 bug with dateCreated auto timestamp, see: https://jira.grails.org/browse/GPHIB-30
        compile "javax.validation:validation-api:1.1.0.Final"
        runtime "org.hibernate:hibernate-validator:5.0.3.Final"
        //END fix for https://jira.grails.org/browse/GPHIB-30
        runtime 'com.h2database:h2:1.4.193'
    }
}
grails.war.resources = { stagingDir, args ->
    delete(file: "${stagingDir}/WEB-INF/lib/jetty-all-9.0.7.v20131107.jar")
    delete(file: "${stagingDir}/WEB-INF/lib/rundeck-jetty-server-${rundeckVersion}.jar")
    delete(file: "${stagingDir}/WEB-INF/lib/servlet-api-2.5.jar")
    if(System.getProperty('rundeck.war.additional')!=null){
        copy(todir: stagingDir ){
            fileset(dir: System.getProperty('rundeck.war.additional'))
        }
    }
}
