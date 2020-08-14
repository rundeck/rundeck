package rundeckapp

import com.dtolabs.rundeck.core.properties.CoreConfigurationPropertiesLoader
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.rundeck.app.bootstrap.PreBootstrap
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertiesPropertySource
import rundeckapp.cli.CommandLineSetup
import rundeckapp.init.DefaultRundeckConfigPropertyLoader
import rundeckapp.init.RundeckInitConfig
import rundeckapp.init.RundeckInitializer

import java.nio.file.Files
import java.nio.file.Paths

@EnableAutoConfiguration(exclude = [SecurityFilterAutoConfiguration])
class Application extends GrailsAutoConfiguration implements EnvironmentAware {
    static final String SYS_PROP_RUNDECK_CONFIG_INITTED = "rundeck.config.initted"
    static RundeckInitConfig rundeckConfig = null
    static void main(String[] args) {
        rundeckConfig = new RundeckInitConfig()
        CommandLineSetup cliSetup = new CommandLineSetup()
        rundeckConfig.cliOptions = cliSetup.runSetup(args)
        runPreboostrap()
        GrailsApp.run(Application, args)
    }

    @Override
    void setEnvironment(final Environment environment) {
        //initialization goes here
        if(rundeckConfig == null) {
            rundeckConfig = new RundeckInitConfig()
            rundeckConfig.cliOptions = new CommandLineSetup().runSetup()
        }
        rundeckConfig.appVersion = environment.getProperty("info.app.version")

        initialize()
        loadAddons()
        loadJdbcDrivers()
        Properties rundeckConfigs = loadRundeckPropertyFile()

        rundeckConfigs.setProperty("rundeck.useJaas", rundeckConfig.useJaas.toString())
        rundeckConfigs.setProperty(
                "rundeck.security.fileUserDataSource",
                rundeckConfig.runtimeConfiguration.getProperty(RundeckInitializer.PROP_REALM_LOCATION)
        )
        rundeckConfigs.setProperty(
                "rundeck.security.jaasLoginModuleName",
                rundeckConfig.runtimeConfiguration.getProperty(RundeckInitializer.PROP_LOGINMODULE_NAME)
        )
        environment.propertySources.addFirst(
                new PropertiesPropertySource(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION, rundeckConfigs)
        )
        loadGroovyRundeckConfigIfExists(environment)
    }

    static void runPreboostrap() {
        ServiceLoader<PreBootstrap> preBootstraps = ServiceLoader.load(PreBootstrap)
        List<PreBootstrap> preboostraplist = []
        preBootstraps.each { pbs -> preboostraplist.add(pbs) }
        preboostraplist.sort { a,b -> a.order <=> b.order }
        preboostraplist.each { pbs ->
            try {
                pbs.run()
            } catch(Exception ex) {
                System.err.println("PreBootstrap process "+pbs.class.canonicalName+" failed")
                ex.printStackTrace()
            }
        }
    }

    void initialize() {
        new RundeckInitializer(rundeckConfig).initialize()
    }

    def loadRundeckPropertyFile() {
        if (!System.getProperty(SYS_PROP_RUNDECK_CONFIG_INITTED) && !System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION).endsWith(".groovy")) {
            CoreConfigurationPropertiesLoader rundeckConfigPropertyFileLoader = new DefaultRundeckConfigPropertyLoader()
            ServiceLoader<CoreConfigurationPropertiesLoader> rundeckPropertyLoaders = ServiceLoader.load(
                    CoreConfigurationPropertiesLoader
            )
            rundeckPropertyLoaders.each { loader ->
                rundeckConfigPropertyFileLoader = loader
            }
            return rundeckConfigPropertyFileLoader.loadProperties()
        }
        return new Properties()
    }

    def loadAddons() {
        File addonDir = new File(rundeckConfig.serverBaseDir, "addons")
        if (addonDir.exists()) {
            addonDir.eachFile { file ->
                Thread.currentThread().contextClassLoader.addURL(file.toURI().toURL())
            }
        }
    }

    def loadJdbcDrivers() {
        File serverLib = new File(rundeckConfig.serverBaseDir, "lib")
        if (serverLib.exists()) {
            serverLib.eachFile { file ->
                if (!file.name.startsWith("rundeck-core") && file.name.endsWith(".jar")) {
                    Thread.currentThread().contextClassLoader.addURL(file.toURI().toURL())
                }
            }
        }
    }

    void loadGroovyRundeckConfigIfExists(final Environment environment) {
        if(System.getProperty(SYS_PROP_RUNDECK_CONFIG_INITTED)) {
            println "Not loading rundeck-config.properties or rundeck-config.groovy because Rundeck config initialization has already taken place"
            return
        }
        String rundeckGroovyConfigFile = System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR) +
                "/rundeck-config.groovy"

        if (System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION) && System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION).endsWith(".groovy")) {
            // if SYS_PROP_RUNDECK_CONFIG_LOCATION is set, get .groovy file from there
            rundeckGroovyConfigFile = System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)
        }

        if (Files.exists(Paths.get(rundeckGroovyConfigFile))) {
            def config = new ConfigSlurper().parse(new File(rundeckGroovyConfigFile).toURL())
            environment.propertySources.addFirst(new MapPropertySource("rundeck-config-groovy", config))
        }

    }
}
