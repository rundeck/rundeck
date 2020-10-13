package rundeckapp

import com.dtolabs.rundeck.core.properties.CoreConfigurationPropertiesLoader
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import org.grails.build.parsing.CommandLineParser
import org.grails.plugins.databasemigration.DatabaseMigrationTransactionManager
import org.grails.plugins.databasemigration.command.DbmGormDiffCommand
import org.grails.plugins.databasemigration.command.DbmRollbackCommand
import org.grails.plugins.databasemigration.command.DbmChangelogSyncCommand
import org.grails.plugins.databasemigration.command.DbmTagCommand
import org.grails.plugins.databasemigration.command.DbmUpdateCommand
import org.grails.plugins.databasemigration.liquibase.GormDatabase
import org.grails.plugins.databasemigration.liquibase.GrailsLiquibase
import org.rundeck.app.bootstrap.PreBootstrap
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertiesPropertySource
import rundeckapp.init.DefaultRundeckConfigPropertyLoader
import rundeckapp.init.RundeckInitConfig
import rundeckapp.init.RundeckInitializer

import javax.sql.DataSource
import java.nio.file.Files
import java.nio.file.Paths

@EnableAutoConfiguration(exclude = [SecurityFilterAutoConfiguration])
class Application extends GrailsAutoConfiguration implements EnvironmentAware {
    static final String SYS_PROP_RUNDECK_CONFIG_INITTED = "rundeck.config.initted"
    static RundeckInitConfig rundeckConfig = null
    static ConfigurableApplicationContext ctx;
    static String[] startArgs = []
    static void main(String[] args) {
        Application.startArgs = args
        runPreboostrap()
        ctx = GrailsApp.run(Application, args)
    }

    static void restartServer() {
        ApplicationArguments args = ctx.getBean(ApplicationArguments.class);
        Thread thread = new Thread({
            ctx.getBean("quartzScheduler").shutdown(true)
            ctx.close()
            ctx = GrailsApp.run(Application, args.getSourceArgs())
        })
        thread.setDaemon(false)
        thread.start()
    }

    @Override
    void setEnvironment(final Environment environment) {
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
    @Override
    void doWithApplicationContext() {
        def cfg = new ConfigObject()

        File migrationsDir = new File(rundeckConfig.serverBaseDir, "migrations")
        if(!migrationsDir.exists())
            migrationsDir.mkdir()

        cfg.grails.plugin.databasemigration.changelogLocation = migrationsDir.absolutePath
        cfg.grails.plugin.databasemigration.changelogFileName = "diff-${grailsApplication.metadata['build.ident']}.groovy"
        grailsApplication.config.merge(cfg)

        if(rundeckConfig.isRollback()) {
            rollback(migrationsDir)
            System.exit(0)
        }
        if(rundeckConfig.isDbSync()) {

            dbsync(migrationsDir)
            System.exit(0)
        }

        createDiff(migrationsDir)
        runDiff()
        runMigrations()


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

    def runMigrations(){
        def updateOnStart = config.getProperty("grails.plugin.databasemigration.updateOnStart", Boolean, false)
        if(updateOnStart) {
            new DatabaseMigrationTransactionManager(applicationContext, 'dataSource').withTransaction {
                GrailsLiquibase gl = new GrailsLiquibase(applicationContext)
                gl.dataSource = applicationContext.getBean('dataSource', DataSource)
                gl.changeLog = config.getProperty("grails.plugin.migration.updateOnStartFileName", String)
                gl.dataSourceName = 'dataSource'
                gl.afterPropertiesSet()
            }
        }
    }
    def runDiff(){
        String diffFileName = "diff-${grailsApplication.metadata['build.ident']}.groovy"
        String tagName = grailsApplication.metadata['build.ident']
        DbmTagCommand tagCommand = new DbmTagCommand()
        tagCommand.applicationContext = applicationContext
        tagCommand.commandLine = new CommandLineParser().parse([tagName] as String[])
        tagCommand.args.add(tagName)
        tagCommand.handle()
        DbmUpdateCommand updateCommand = new DbmUpdateCommand()
        updateCommand.applicationContext = applicationContext
        updateCommand.commandLine = new CommandLineParser().parse([diffFileName] as String[])
        updateCommand.args.add(diffFileName)
        updateCommand.handle()

    }

    def createDiff(File migrationsDir){
        String diffFileName = "diff-${grailsApplication.metadata['build.ident']}.groovy"
        File diffFile = new File(migrationsDir.absolutePath, diffFileName)
        if (diffFile.exists() && grails.util.Environment.PRODUCTION != grails.util.Environment.getCurrent()){
            diffFileName = "diff-${grailsApplication.metadata['build.ident']}-${new Date().format("yyyy-MM-dd'T'HH:mm")}.groovy"
            diffFile = new File(migrationsDir.absolutePath, diffFileName)
        }
        else if (diffFile.exists())
            return

        DbmGormDiffCommand diffCommand = new DbmGormDiffCommand()
        diffCommand.applicationContext = applicationContext
        diffCommand.commandLine = new CommandLineParser().parse([diffFile.name] as String[])
        diffCommand.args.add(diffFile.name)
        diffCommand.commandLine.undeclaredOptions.put("add", true)
        diffCommand.handle()

    }

    def rollback(File migrationsDir){
        migrationsDir.eachFile { file ->
            def newconf = new ConfigObject()
            newconf.grails.plugin.databasemigration.changelogFileName = file.name
            grailsApplication.config.merge(newconf)

            DbmRollbackCommand rollbackCommand = new DbmRollbackCommand()
            rollbackCommand.applicationContext = applicationContext
            rollbackCommand.commandLine = new CommandLineParser().parse([rundeckConfig.tagName()] as String[])
            rollbackCommand.args.add(rundeckConfig.tagName())
            rollbackCommand.handle()
        }
    }

    def dbsync(File migrationsDir){
        migrationsDir.eachFile { file ->
            def newconf = new ConfigObject()
            newconf.grails.plugin.databasemigration.changelogFileName = file.name
            grailsApplication.config.merge(newconf)

            DbmChangelogSyncCommand changelogSyncCommand = new DbmChangelogSyncCommand();
            changelogSyncCommand.applicationContext = applicationContext
            changelogSyncCommand.handle()
        }
    }

}
