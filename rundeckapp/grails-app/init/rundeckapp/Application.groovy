package rundeckapp

import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.core.env.PropertiesPropertySource
import rundeckapp.cli.CommandLineSetup
import rundeckapp.init.RundeckInitConfig
import rundeckapp.init.RundeckInitializer

class Application extends GrailsAutoConfiguration implements EnvironmentAware {
    static RundeckInitConfig rundeckConfig = null
    static void main(String[] args) {
        rundeckConfig = new RundeckInitConfig()
        CommandLineSetup cliSetup = new CommandLineSetup()
        rundeckConfig.cliOptions = cliSetup.runSetup(args)
        GrailsApp.run(Application, args)
    }

    @Override
    void onStartup(final Map<String, Object> event) {
        super.onStartup(event)

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

        Properties rundeckConfigs = new Properties()
        rundeckConfigs.load(new File(System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)).newInputStream())
        rundeckConfigs.setProperty("rundeck.useJaas", rundeckConfig.useJaas.toString())
        environment.propertySources.addFirst(new PropertiesPropertySource(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION, rundeckConfigs))

    }

}