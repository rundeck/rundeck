package testhelper

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import grails.orm.bootstrap.HibernateDatastoreSpringInitializer
import grails.test.hibernate.HibernateSpec
import groovy.sql.Sql
import liquibase.parser.ChangeLogParserFactory
import org.grails.config.PropertySourcesConfig
import org.grails.orm.hibernate.cfg.Settings
import org.grails.plugins.databasemigration.DatabaseMigrationTransactionManager
import org.grails.plugins.databasemigration.liquibase.GrailsLiquibase
import org.grails.plugins.databasemigration.liquibase.GroovyChangeLogParser
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources

import javax.sql.DataSource

import static java.util.concurrent.TimeUnit.SECONDS

class RundeckHibernateSpec extends HibernateSpec {
    Sql sql

    def tableList = ['AUTH_TOKEN', 'BASE_REPORT', 'EXECUTION', 'JOB_FILE_RECORD', 'LOG_FILE_STORAGE_REQUEST',
                     'NOTIFICATION', 'ORCHESTRATOR', 'PLUGIN_META', 'PROJECT', 'RDUSER', 'RDOPTION',
                     'RDOPTION_VALUES', 'REFERENCED_EXECUTION', 'SCHEDULED_EXECUTION',
                     'SCHEDULED_EXECUTION_STATS', 'STORAGE', 'STORED_EVENT', 'WEBHOOK',
                     'WORKFLOW', 'WORKFLOW_STEP', 'WORKFLOW_WORKFLOW_STEP']

    Map getConfiguration() {
        Collections.singletonMap(Settings.SETTING_DB_CREATE, "none")
    }

    def setupSpec() {
        dropAllObjects()
        runMigrations(createNewDataSource(hibernateDatastore.connectionSources.configuration.dataSource))
    }

    def runMigrations(DataSource dataSource){
        GenericApplicationContext applicationContext = configureApplicationContext(dataSource)
        sql = new Sql(dataSource)
        List rows = sql.rows('SELECT * FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = \'DATABASECHANGELOG\'')
        if(rows?.size() > 0) {
            sql.execute('DELETE FROM DATABASECHANGELOG')
            sql.execute('DELETE FROM DATABASECHANGELOGLOCK')
            tableList.each { table ->
                String statement = "DROP TABLE IF EXISTS ${table} CASCADE"
                sql.execute(statement)
            }
        }

        new DatabaseMigrationTransactionManager(applicationContext, 'dataSource').withTransaction {
            GrailsLiquibase gl = new GrailsLiquibase(applicationContext)
            gl.dataSource = dataSource
            gl.changeLog = 'changelog.groovy'
            gl.dataSourceName = 'dataSource'
            gl.afterPropertiesSet()
        }
    }

    def cleanupSpec(){
        dropAllObjects()
    }

    def dropAllObjects(){
        sql = new Sql(createNewDataSource(hibernateDatastore.connectionSources.configuration.dataSource))
        sql.execute("DROP ALL OBJECTS")
    }

    private void registerLogParser(def config, GenericApplicationContext applicationContext){
        GroovyChangeLogParser groovyChangeLogParser = new GroovyChangeLogParser()
        groovyChangeLogParser.applicationContext = applicationContext
        groovyChangeLogParser.config = config
        ChangeLogParserFactory.instance.unregisterAllParsers()
        ChangeLogParserFactory.instance.register( groovyChangeLogParser )
    }

    private GenericApplicationContext configureApplicationContext(DataSource hikariDataSource){
        GenericApplicationContext genericApplicationContext = new GenericApplicationContext()
        genericApplicationContext.beanFactory.registerSingleton('dataSource', hikariDataSource)
        def mutablePropertySources = new MutablePropertySources()
        mutablePropertySources.addFirst(new MapPropertySource('dataSourceConfig', [
                'grails.plugin.databasemigration.changelogLocation': 'grails-app/migrations',
                'dataSource.dbCreate'                              : 'none',
                'dataSource.url'                                   : hikariDataSource.jdbcUrl,
                'dataSource.username'                              : hikariDataSource.username,
                'dataSource.password'                              : hikariDataSource.password,
                'dataSource.driverClassName'                       : hikariDataSource.driverClassName,
                'environments.other.dataSource.url'                : hikariDataSource.jdbcUrl,
        ]))
        def config = new PropertySourcesConfig(mutablePropertySources)
        def datastoreInitializer = new HibernateDatastoreSpringInitializer(config, [] as Class[])
        datastoreInitializer.configureForBeanDefinitionRegistry(genericApplicationContext)
        genericApplicationContext.refresh()

        registerLogParser(config, genericApplicationContext)

        return genericApplicationContext
    }

    private HikariDataSource createNewDataSource(def dataSourceConfig){
        return new HikariDataSource(new HikariConfig(jdbcUrl: dataSourceConfig.url, username: dataSourceConfig.username,
                password: dataSourceConfig.password , driverClassName: dataSourceConfig.driverClassName, maxPoolSize: 2, idleTimeout: 10000L,
                initializationFailTimeout: SECONDS.toMillis(120)))
    }
}
