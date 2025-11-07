package rundeckapp

import com.dtolabs.rundeck.app.api.ApiVersions
import grails.boot.GrailsApp
import grails.boot.config.GrailsAutoConfiguration
import groovy.util.logging.Slf4j
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.security.SecurityScheme
import io.swagger.v3.oas.annotations.servers.Server
import io.swagger.v3.oas.annotations.servers.ServerVariable
import io.swagger.v3.oas.annotations.tags.Tag
import liquibase.exception.LockException
import org.rundeck.app.bootstrap.PreBootstrap
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.Environment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertiesPropertySource
import rundeckapp.init.ReloadableRundeckPropertySource
import rundeckapp.init.RundeckInitConfig
import rundeckapp.init.RundeckInitializer
import rundeckapp.init.RundeckDbMigration
import rundeckapp.init.prebootstrap.InitializeRundeckPreboostrap

import java.nio.file.Files
import java.nio.file.Paths

@OpenAPIDefinition(
    info = @Info(
    title = "Rundeck / Runbook Automation API",
    version = ApiVersions.API_CURRENT_VERSION_STR,
    description = "Rundeck / Runbook Automation REST API for job automation, execution management, and system administration.\n\n" +
              "The Rundeck API provides comprehensive access to:\n" +
              "- Job management (create, update, delete, execute jobs)\n" +
              "- Execution monitoring and control\n" +
              "- Project and resource management\n" +
              "- Node filtering and resource queries\n" +
              "- System configuration and administration\n" +
              "- SCM integration (Git and other version control)\n" +
              "- Authentication token management\n" +
              "- Metrics and health monitoring\n\n" +
              "All API endpoints require authentication via API token, password session, or JWT token (Enterprise).\n" +
              "API version must be specified in the URL path (e.g., /api/46/...).\n\n" +
              "For detailed documentation, see: [Rundeck API Docs](https://docs.rundeck.com/docs/api/)"
    ),
    externalDocs = @ExternalDocumentation(
        description = 'Original Rundeck API Documentation',
        url = 'https://docs.rundeck.com/docs/api/'
    ),
    servers = [
        @Server(
            url = "{protocol}://{host}:{port}/api/{version}",
            description = "Rundeck / Runbook Automation Server",
            variables = [
                @ServerVariable(name = "protocol", defaultValue = "https", allowableValues = ["http", "https"], description = "Protocol (http or https)"),
                @ServerVariable(name = "host", defaultValue = "localhost", description = "Server hostname or IP address"),
                @ServerVariable(name = "port", defaultValue = "4440", description = "Server port number"),
                @ServerVariable(name = "version", defaultValue = ApiVersions.API_CURRENT_VERSION_STR, description = "API version number")
            ]
        )
    ],
    security = [
        @SecurityRequirement(name = "rundeckApiToken"),
        @SecurityRequirement(name = "rundeckPassword"),
        @SecurityRequirement(name = "rundeckJWT")
    ],
    tags = [
        @Tag(name = "ACL", description = "Access Control List operations"),
        @Tag(name = "Ad Hoc", description = "Ad-hoc command execution operations"),
        @Tag(name = "API", description = "API information and utilities"),
        @Tag(name = "Authorization", description = "Check User Authorization operations"),
        @Tag(name = "Calendars", description = "Calendar management operations"),
        @Tag(name = "Cluster", description = "Cluster management operations"),
        @Tag(name = "Configuration", description = "Configuration management"),
        @Tag(name = "Execution Mode", description = "Execution Mode operations"),
        @Tag(name = "Health", description = "Health check operations"),
        @Tag(name = "History", description = "Execution history operations"),
        @Tag(name = "Jobs", description = "Job management operations"),
        @Tag(name = "Job Executions", description = "Job execution operations"),
        @Tag(name = "Key Storage", description = "Key storage operations"),
        @Tag(name = "License", description = "License management operations"),
        @Tag(name = "Log Storage", description = "Log storage operations"),
        @Tag(name = "Metrics", description = "Metrics and monitoring operations"),
        @Tag(name = "Plugins", description = "Plugin management operations"),
        @Tag(name = "Project", description = "Project management operations"),
        @Tag(name = "ROI", description = "Return on Investment metrics operations"),
        @Tag(name = "Runner", description = "Runner management operations"),
        @Tag(name = "SCM", description = "Source Control Management operations"),
        @Tag(name = "System", description = "System operations"),
        @Tag(name = "Tokens", description = "API token operations"),
        @Tag(name = "Tours", description = "User interface tour operations"),
        @Tag(name = "User", description = "User management operations"),
        @Tag(name = "User Class", description = "User Class management operations"),
        @Tag(name = "Webhook", description = "Webhook operations")
    ]
)
@SecurityScheme(
    name = "rundeckApiToken",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "X-Rundeck-Auth-Token",
    description = "API Token authentication. Include your API token in the X-Rundeck-Auth-Token header or as an 'authtoken' query parameter. Tokens can be generated from your User Profile page and must have appropriate authorization roles and expiration settings."
)
@SecurityScheme(
    name = "rundeckPassword",
    type = SecuritySchemeType.HTTP,
    scheme = "basic",
    description = "Session-based authentication using username and password. Submit credentials to /j_security_check and maintain JSESSIONID cookie."
)
@SecurityScheme(
    name = "rundeckJWT",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT token authentication for OAuth/OIDC integration (Commercial/Enterprise only). Include JWT token in Authorization header with Bearer schema."
)
@EnableAutoConfiguration(exclude = [SecurityFilterAutoConfiguration])
@Slf4j
class Application extends GrailsAutoConfiguration implements EnvironmentAware {
    static final String SYS_PROP_RUNDECK_CONFIG_INITTED = "rundeck.config.initted"
    static RundeckInitConfig rundeckConfig = null
    static ConfigurableApplicationContext ctx;
    static String[] startArgs = []
    static void main(String[] args) {
        Application.startArgs = args
        boolean error = runPrebootstrap()
        if(error) {
            System.err.println("Rundeck initialization failed")
            exitWithCode(1)
        }
        boolean startupException = false
        try {
            execRunApp()
        } catch(Exception ex) {
            startupException = true
        }
        if(rundeckConfig.isMigrate()) {
            println startupException ? "\nError encountered when running migrations" : "\nMigrations complete"
            exitWithCode(startupException ? 1 : 0)
        }
    }

    static void execRunApp(String[] args) {
        ctx = GrailsApp.run(Application, args)
    }

    static void exitWithCode(Integer code) {
        System.exit(code)
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
        Properties hardCodedRundeckConfigs = new Properties()
        if(rundeckConfig == null) Application.runPrebootstrap()

        hardCodedRundeckConfigs.setProperty("rundeck.useJaas", rundeckConfig.useJaas.toString())
        hardCodedRundeckConfigs.setProperty(
                "rundeck.security.fileUserDataSource",
                rundeckConfig.runtimeConfiguration.getProperty(RundeckInitializer.PROP_REALM_LOCATION)
        )
        hardCodedRundeckConfigs.setProperty(
                "rundeck.security.jaasLoginModuleName",
                rundeckConfig.runtimeConfiguration.getProperty(RundeckInitializer.PROP_LOGINMODULE_NAME)
        )
        environment.propertySources.addFirst(
                new PropertiesPropertySource("hardcoded-rundeck-props", hardCodedRundeckConfigs)
        )
        environment.propertySources.addFirst(ReloadableRundeckPropertySource.getRundeckPropertySourceInstance())
        if(rundeckConfig.migrate) {
            environment.propertySources.addFirst(new MapPropertySource("ensure-migration-flag",["grails.plugin.databasemigration.updateOnStart":true]))
        }
        loadGroovyRundeckConfigIfExists(environment)
        removeGORMdbCreateProperty(environment)
    }

    /**
     * It sets dataSource.dbCreate as 'none', always
     * @param environment
     * @return void
     */
    void removeGORMdbCreateProperty(final Environment environment){
        if(environment && environment.propertySources){
            environment.propertySources.each{
                if(it.containsProperty("dataSource.dbCreate")){
                    if(it.source && it.source instanceof Properties){
                        it.source.'dataSource.dbCreate' = 'none'
                    }
                }
            }
        }
    }


    @Override
    void doWithApplicationContext() {
        if(rundeckConfig.isRollback()) {
            RundeckDbMigration rundeckDbMigration = new RundeckDbMigration(applicationContext)
            println "Beginning db rollback to ${rundeckConfig.tagName()}"
            rundeckDbMigration.rollback(rundeckConfig.tagName())
            println "Rollback complete"
            System.exit(0)
        }
    }

    void doWithDynamicMethods() {
    }
    static boolean runPrebootstrap() {
        List<PreBootstrap> preboostraplist = getPrebootstrapFunctions()
        preboostraplist.sort { a,b -> a.order <=> b.order }
        boolean error = false;
        preboostraplist.each { pbs ->
            try {
                pbs.run()
            }catch(LockException le){
                log.error("Cannot obtain lock on table DATABASECHANGELOGLOCK. This could be due to a database connection issue when starting a Process Automation instance. " +
                        "To force the unlock you must first make sure that are no Process Automation instances running using this DB. " +
                        "You can change the lock status directly on the database and start Process Automation again by setting 'LOCKED' field in the table 'DATABASECHANGELOGLOCK' as false (or 0 depending on the database)", le)
                error = true
            }
            catch(Exception ex) {
                System.err.println("PreBootstrap process "+pbs.class.canonicalName+" failed")
                ex.printStackTrace()
                error = true
            }
        }
        return error
    }

    static List<PreBootstrap> getPrebootstrapFunctions() {
        ServiceLoader<PreBootstrap> preBootstraps = ServiceLoader.load(PreBootstrap)
        List<PreBootstrap> preboostraplist = []
        preBootstraps.each { pbs -> preboostraplist.add(pbs) }
        return preboostraplist
    }


    void loadGroovyRundeckConfigIfExists(final Environment environment) {
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
