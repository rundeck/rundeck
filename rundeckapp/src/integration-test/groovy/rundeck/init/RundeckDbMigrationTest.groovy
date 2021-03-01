package rundeck.init

import grails.testing.mixin.integration.Integration
import groovy.sql.Sql
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

    def setup() {
    }

    def cleanupSpec() {
     }

    void "run diff generates database with schema"(){
        when:
        RundeckDbMigration rundeckDbMigration = new RundeckDbMigration(applicationContext, grailsApplication)
        rundeckDbMigration.runMigrations()

        then:
        sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG').num > 0
        def tables = sql.rows(' select * from information_schema.tables')
        tables*.TABLE_NAME.contains("AUTH_TOKEN")
        tables*.TABLE_NAME.contains("BASE_REPORT")
        tables*.TABLE_NAME.contains("EXECUTION")
        tables*.TABLE_NAME.contains("JOB_FILE_RECORD")
        tables*.TABLE_NAME.contains("LOG_FILE_STORAGE_REQUEST")
        tables*.TABLE_NAME.contains("NOTIFICATION")
        tables*.TABLE_NAME.contains("NODE_FILTER")
        tables*.TABLE_NAME.contains("ORCHESTRATOR")
        tables*.TABLE_NAME.contains("PROJECT")
        tables*.TABLE_NAME.contains("RDUSER")
        tables*.TABLE_NAME.contains("RDOPTION")
        tables*.TABLE_NAME.contains("RDOPTION_VALUES")
        tables*.TABLE_NAME.contains("REFERENCED_EXECUTION")
        tables*.TABLE_NAME.contains("REPORT_FILTER")
        tables*.TABLE_NAME.contains("SCHEDULED_EXECUTION")
        tables*.TABLE_NAME.contains("SCHEDULED_EXECUTION_FILTER")
        tables*.TABLE_NAME.contains("SCHEDULED_EXECUTION_STATS")
        tables*.TABLE_NAME.contains("STORAGE")
        tables*.TABLE_NAME.contains("WEBHOOK")
        tables*.TABLE_NAME.contains("WORKFLOW")
        tables*.TABLE_NAME.contains("WORKFLOW_STEP")
        tables*.TABLE_NAME.contains("WORKFLOW_WORKFLOW_STEP")

    }

}
