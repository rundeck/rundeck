/*
 * The following allows grails to leverage a different url setting for maven central. This would
 * typically be passed along as a -D parameter to grails, ie: grails -Dmaven.central.url=http://...
 */
def mavenCentralUrl = 'http://repo1.maven.org/maven2/'
if (System.properties['maven.central.url']) {
    mavenCentralUrl = System.properties['maven.central.url']
}
println "Maven Central: ${mavenCentralUrl}"


grails.project.target.level = 1.7
grails.project.source.level = 1.7

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
        test    ':code-coverage:2.0.3-3'
        compile (':less-asset-pipeline:2.0.8')
        compile ':twitter-bootstrap:3.3.2.1'
        compile (':asset-pipeline:2.0.8')
        compile ':cache:1.1.8'
        compile ":platform-core:1.0.0"
        runtime (':hibernate4:4.3.6.1')
        runtime ':mail:1.0.7', ':quartz:1.0.2', ':executor:0.3'

        runtime ':profiler:0.5'
        runtime ':miniprofiler:0.4.1'
        provided ':codenarc:0.22'
        build   ':jetty:2.0.3'
    }

    dependencies {

        build 'org.yaml:snakeyaml:1.9'
        compile 'org.yaml:snakeyaml:1.9', 'org.apache.ant:ant:1.8.3', 'org.apache.ant:ant-jsch:1.8.3',
                'com.jcraft:jsch:0.1.53', 'log4j:log4j:1.2.17', 'commons-collections:commons-collections:3.2.2',
                'commons-codec:commons-codec:1.5',
                'com.fasterxml.jackson.core:jackson-databind:2.5.3',
                'com.fasterxml.jackson.core:jackson-annotations:2.5.3',
                'com.codahale.metrics:metrics-core:3.0.1', 'com.google.guava:guava:15.0',
                'org.owasp.encoder:encoder:1.1.1', 'org.quartz-scheduler:quartz:2.2.1',
                'org.markdownj:markdownj-core:0.4',
                'com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:r239'

        compile("org.rundeck:rundeck-core:${rundeckVersion}")
        compile("org.rundeck:rundeck-storage-filesys:${rundeckVersion}")

        runtime 'postgresql:postgresql:9.1-901.jdbc4'
        runtime 'mysql:mysql-connector-java:5.1.35'

        //BEGIN fix hibernate4 bug with dateCreated auto timestamp, see: https://jira.grails.org/browse/GPHIB-30
        compile "javax.validation:validation-api:1.1.0.Final"
        runtime "org.hibernate:hibernate-validator:5.0.3.Final"
        //END fix for https://jira.grails.org/browse/GPHIB-30
    }
}
grails.war.resources = { stagingDir, args ->
    delete(file: "${stagingDir}/WEB-INF/lib/jetty-all-7.6.0.v20120127.jar")
    delete(file: "${stagingDir}/WEB-INF/lib/rundeck-jetty-server-${rundeckVersion}.jar")
    delete(file: "${stagingDir}/WEB-INF/lib/servlet-api-2.5.jar")
    if(System.getProperty('rundeck.war.additional')!=null){
        copy(todir: stagingDir ){
            fileset(dir: System.getProperty('rundeck.war.additional'))
        }
    }
}
