/*
 * The following allows grails to leverage a different url setting for maven central. This would
 * typically be passed along as a -D parameter to grails, ie: grails -Dmaven.central.url=http://...
 */
def mavenCentralUrl = "http://repo1.maven.org/maven2/"
if (System.properties["maven.central.url"]) {
    mavenCentralUrl = System.properties["maven.central.url"]
}
println "Maven Central: ${mavenCentralUrl}"

Boolean mavenCredsDefined = false
def mavenRealm
def mavenHost
def mavenUser
def mavenPassword

// TODO: System.env["mavenRealm"] is a hack.  See comments below.
if (System.env["mavenRealm"] && System.properties["maven.host"] && System.properties["maven.user"] && System.properties["maven.password"]) {
    mavenCredsDefined = true

    /*
     * There's a bug in grails 1.3.7 (fixed in 2.0.0) where system properties 
     * (e.g. -Dmaven.realm="Sonatype Nexus Repository Manager") are truncated at the first space
     * (e.g. System.properties["maven.realm"] is "Sonatype")
     */
    // mavenRealm = System.properties["maven.realm"]

    /*
     * Fortunately, the bug doesn't affect reading environment variables.
     * TODO: This is a hack until grails is upgraded to something more recent...
     */
    mavenRealm = System.env["mavenRealm"]

    mavenHost = System.properties["maven.host"]
    mavenUser = System.properties["maven.user"]
    mavenPassword = System.properties["maven.password"]

    println "Maven credentials:\n\tRealm: ${mavenRealm}\n\tHost: ${mavenHost}\n\tUser: ${mavenUser}"
}

def grailsLocalRepo = "grails-app/plugins"
if (System.properties["grails.local.repo"]) {
        grailsLocalRepo = System.properties["grails.local.repo"]
}
println "Grails Local Repo: ${grailsLocalRepo}"

grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        useOrigin true
        mavenLocal()
        flatDir name:'grailsLocalRepo', dirs:"${grailsLocalRepo}"
        grailsHome()
        grailsPlugins()
        mavenRepo mavenCentralUrl
        grailsCentral()
    }

    if (mavenCredsDefined) {
        credentials {
            realm = mavenRealm
            host = mavenHost
            username = mavenUser
            password = mavenPassword
        }
    }

    grails.war.resources = {def stagingDir ->
        delete(file: "${stagingDir}/WEB-INF/lib/commons-collections-3.1.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/servlet-api-2.5-20081211.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/jetty-6.1.21.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/jetty-plus-6.1.21.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/jetty-util-6.1.21.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/jetty-naming-6.1.21.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/jsp-api-2.0-6.1.21.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/jasper-runtime-5.5.15.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/jasper-compiler-5.5.15.jar")
        delete(file: "${stagingDir}/WEB-INF/lib/jasper-compiler-jdt-5.5.15.jar")
    }

    rundeckVersion = System.getProperty("RUNDECK_VERSION", appVersion)
    println "Application Version: ${rundeckVersion}"
	
    dependencies {
        
        test 'org.yaml:snakeyaml:1.9', 'org.apache.ant:ant:1.7.1', 'org.apache.ant:ant-jsch:1.7.1', 
             'com.jcraft:jsch:0.1.45', 'log4j:log4j:1.2.16', 'commons-collections:commons-collections:3.2.1', 
             'commons-codec:commons-codec:1.5', 'com.fasterxml.jackson.core:jackson-databind:2.0.2'
        test("org.rundeck:rundeck-core:${rundeckVersion}"){
            changing=true
        }
             
        compile 'org.yaml:snakeyaml:1.9', 'org.apache.ant:ant:1.7.1', 'org.apache.ant:ant-jsch:1.7.1', 
                'com.jcraft:jsch:0.1.45','log4j:log4j:1.2.16','commons-collections:commons-collections:3.2.1',
                'commons-codec:commons-codec:1.5', 'com.fasterxml.jackson.core:jackson-databind:2.0.2'
        compile("org.rundeck:rundeck-core:${rundeckVersion}") {
            changing = true
            excludes("xalan")
        }

        runtime 'org.yaml:snakeyaml:1.9', 'org.apache.ant:ant:1.7.1', 'org.apache.ant:ant-launcher:1.7.1',
                'org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.45', 'org.springframework:spring-test:3.0.5.RELEASE',
                'log4j:log4j:1.2.16' ,'commons-collections:commons-collections:3.2.1','commons-codec:commons-codec:1.5', 
                'com.fasterxml.jackson.core:jackson-databind:2.0.2', 'postgresql:postgresql:9.1-901.jdbc4'
        runtime("org.rundeck:rundeck-core:${rundeckVersion}") {
            changing = true
        }
        runtime("org.rundeck:rundeck-jetty-server:${rundeckVersion}") {
            changing = true
        }
    }
    
    grails.plugin.location.'webrealms' = "webrealms"
}
