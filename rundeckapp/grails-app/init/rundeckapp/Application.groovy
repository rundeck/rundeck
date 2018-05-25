package rundeckapp

import com.dtolabs.rundeck.core.properties.CoreConfigurationPropertiesLoader
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.SecurityFilterAutoConfiguration
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
    static RundeckInitConfig rundeckConfig = null
    static void main(String[] args) {
        rundeckConfig = new RundeckInitConfig()
        CommandLineSetup cliSetup = new CommandLineSetup()
        rundeckConfig.cliOptions = cliSetup.runSetup(args)
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

        RundeckInitializer initilizer = new RundeckInitializer(rundeckConfig)
        initilizer.initialize()
        CoreConfigurationPropertiesLoader rundeckConfigPropertyFileLoader = new DefaultRundeckConfigPropertyLoader()
        ServiceLoader<CoreConfigurationPropertiesLoader> rundeckPropertyLoaders = ServiceLoader.load(
                CoreConfigurationPropertiesLoader
        )
        rundeckPropertyLoaders.each { loader ->
            rundeckConfigPropertyFileLoader = loader
        }
        Properties rundeckConfigs = rundeckConfigPropertyFileLoader.loadProperties()
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

    void loadGroovyRundeckConfigIfExists(final Environment environment) {
        String rundeckGroovyConfigFile = System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR) +
                                         "/rundeck-config.groovy"
        if (Files.exists(Paths.get(rundeckGroovyConfigFile))) {
            def config = new ConfigSlurper().parse(new File(rundeckGroovyConfigFile).toURL())
            environment.propertySources.addFirst(new MapPropertySource("rundeck-config-groovy", config))
        }

    }
}