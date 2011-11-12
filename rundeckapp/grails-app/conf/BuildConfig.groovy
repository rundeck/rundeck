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
    dependencies {
        build 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.42','log4j:log4j:1.2.16'
        test 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.42','log4j:log4j:1.2.16'
        compile 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.42','log4j:log4j:1.2.16'
        runtime 'org.yaml:snakeyaml:1.9', 'org.apache.ant:ant:1.7.1', 'org.apache.ant:ant-launcher:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.42', 'org.springframework:spring-test:3.0.5.RELEASE','log4j:log4j:1.2.16'
    }
    grails.plugin.location.'webrealms' = "webrealms"
}
