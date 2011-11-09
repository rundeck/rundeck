grails.project.dependency.resolution = {
    inherits "global" // inherit Grails' default dependencies
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        //include snakeyaml from deps dir
        flatDir name:'sourceDeps', dirs:'../dependencies/snakeyaml/jars'
        grailsHome()
        mavenCentral()
        mavenLocal()
    }
    dependencies {
        build 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.42'
        test 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.42'
        compile 'org.yaml:snakeyaml:1.9','org.apache.ant:ant:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.42'
        runtime 'org.yaml:snakeyaml:1.9', 'org.apache.ant:ant:1.7.1', 'org.apache.ant:ant-launcher:1.7.1','org.apache.ant:ant-jsch:1.7.1','com.jcraft:jsch:0.1.42', 'org.springframework:org.springframework.test:3.0.5.RELEASE'
    }
    grails.plugin.location.'webrealms' = "webrealms"
}
