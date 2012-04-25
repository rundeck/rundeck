grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
    	mavenCentral()
        grailsPlugins()
        grailsHome()
        grailsCentral()
        mavenLocal()

        //include snakeyaml from deps dir
        flatDir name:'sourceDeps', dirs:'../dependencies/snakeyaml/jars'
    }

    grails.war.resources = {def stagingDir ->
        delete(file: "${stagingDir}/WEB-INF/lib/commons-collections-3.1.jar")
    }

    dependencies {
        build 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.45','log4j:log4j:1.2.16','commons-collections:commons-collections:3.2.1','commons-codec:commons-codec:1.5'
        test 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.45','log4j:log4j:1.2.16','commons-collections:commons-collections:3.2.1','commons-codec:commons-codec:1.5'
        compile 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.45','log4j:log4j:1.2.16','commons-collections:commons-collections:3.2.1','commons-codec:commons-codec:1.5'
        runtime 'org.yaml:snakeyaml:1.9', 'org.apache.ant:ant:1.7.1', 'org.apache.ant:ant-launcher:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.45', 'org.springframework:spring-test:3.0.5.RELEASE','log4j:log4j:1.2.16'    ,'commons-collections:commons-collections:3.2.1','commons-codec:commons-codec:1.5'
    }
    grails.plugin.location.'webrealms' = "webrealms"
}
