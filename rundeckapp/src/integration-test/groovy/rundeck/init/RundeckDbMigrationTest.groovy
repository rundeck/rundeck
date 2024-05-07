package rundeck.init

import grails.testing.mixin.integration.Integration
import groovy.sql.Sql
import org.grails.plugins.databasemigration.DatabaseMigrationTransactionManager
import org.grails.plugins.databasemigration.liquibase.GrailsLiquibase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import rundeckapp.init.RundeckDbMigration
import spock.lang.AutoCleanup
import spock.lang.Specification

import javax.sql.DataSource

@Integration
class RundeckDbMigrationTest extends Specification {

    @Autowired
    DataSource dataSource

    @Autowired
    ApplicationContext applicationContext

    def grailsApplication

    @AutoCleanup
    Sql sql


    def tableList = ['AUTH_TOKEN', 'BASE_REPORT', 'EXECUTION', 'JOB_FILE_RECORD', 'LOG_FILE_STORAGE_REQUEST',
                     'NOTIFICATION', 'ORCHESTRATOR', 'PLUGIN_META', 'PROJECT', 'RDUSER', 'RDOPTION',
                     'REFERENCED_EXECUTION', 'SCHEDULED_EXECUTION',
                     'SCHEDULED_EXECUTION_STATS', 'STORAGE', 'STORED_EVENT', 'WEBHOOK',
                     'WORKFLOW', 'WORKFLOW_STEP', 'WORKFLOW_WORKFLOW_STEP']

    def setup() {
        sql = new Sql(dataSource)
    }

    def cleanupSpec() {
     }

    void "db migrations updates database to the current version"(){
        setup:
        sql.execute('DELETE FROM DATABASECHANGELOG')
        tableList.each{table->
            String statement =  "DROP TABLE ${table} CASCADE"
            sql.execute(statement)
        }

        expect:
        sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG').num == 0

        when:
        runMigrations()

        then:
        sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG').num > 0
        def tables = sql.rows(' select * from information_schema.tables')
        tableList.each{table->
            tables*.TABLE_NAME.contains("${table}")
        }

    }

    void "Rolls back the database to the state it was in when the tag was applied"(){
        given:
        String tagName = "3.4.0"
        RundeckDbMigration rundeckDbMigration = new RundeckDbMigration(applicationContext)
        def numOfChangeSets = sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG').num

        expect:
        sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG').num > 0

        when: "add change set"
        def cfg = new ConfigObject()
        cfg.grails.plugin.databasemigration.changelogLocation = 'src/integration-test/resources'
        cfg.grails.plugin.databasemigration.changelog = 'changelog-test.groovy'
        grailsApplication.config.merge(cfg)
        runMigrations()

        then:
        sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG').num == numOfChangeSets + 1
        sql.rows("select * from information_schema.tables where TABLE_NAME = 'INTEGRATIONTESTTABLE'").size() == 1

        when: "rollback"
        cfg.grails.plugin.databasemigration.changelogFileName = 'changelog-test.groovy'
        grailsApplication.config.merge(cfg)
        rundeckDbMigration.rollback(tagName)

        then:
        sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG').num == numOfChangeSets
        sql.rows("select * from information_schema.tables where TABLE_NAME = 'INTEGRATIONTESTTABLE'").size() == 0

    }

    def runMigrations(){

        new DatabaseMigrationTransactionManager(applicationContext, 'dataSource').withTransaction {
            GrailsLiquibase gl = new GrailsLiquibase(applicationContext)
            gl.dataSource = applicationContext.getBean('dataSource', DataSource)
            gl.changeLog = grailsApplication.config.getProperty("grails.plugin.databasemigration.changelog", String)
            gl.dataSourceName = 'dataSource'
            gl.afterPropertiesSet()
        }
    }
}
