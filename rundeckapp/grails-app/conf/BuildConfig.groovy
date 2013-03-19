grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        useOrigin true
        mavenLocal()
        grailsHome()
        grailsPlugins()
        grailsCentral()
        mavenCentral()
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
        }

        runtime 'org.yaml:snakeyaml:1.9', 'org.apache.ant:ant:1.7.1', 'org.apache.ant:ant-launcher:1.7.1',
                'org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.45', 'org.springframework:spring-test:3.0.5.RELEASE',
                'log4j:log4j:1.2.16' ,'commons-collections:commons-collections:3.2.1','commons-codec:commons-codec:1.5', 
                'com.fasterxml.jackson.core:jackson-databind:2.0.2', 'postgresql:postgresql:9.1-901.jdbc4'
        runtime("org.rundeck:rundeck-core:${rundeckVersion}") {
            changing = true
        }
        runtime("org.rundeck:rundeck-jetty-server:${appVersion}") {
            changing = true
        }
    }
    
    grails.plugin.location.'webrealms' = "webrealms"
}
