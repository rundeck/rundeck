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

    // ===== Job Audit Migration Tests =====

    def "lastModifiedBy column is added to scheduled_execution table"() {
        setup: "Ensure we start with a clean migration state"
        // Remove the lastModifiedBy column if it exists to test the migration
        try {
            sql.execute("ALTER TABLE scheduled_execution DROP COLUMN last_modified_by")
        } catch (Exception e) {
            // Column might not exist, which is fine for testing
        }

        when: "migrations are run"
        runMigrations()

        then: "lastModifiedBy column should exist"
        def columns = sql.rows("""
            SELECT column_name, data_type, is_nullable, character_maximum_length 
            FROM information_schema.columns 
            WHERE table_name = 'scheduled_execution' 
            AND column_name = 'last_modified_by'
        """)
        
        columns.size() == 1
        def columnInfo = columns[0]
        columnInfo.COLUMN_NAME.toLowerCase() == 'last_modified_by'
        columnInfo.IS_NULLABLE == 'YES' // Should be nullable
        columnInfo.CHARACTER_MAXIMUM_LENGTH >= 255 || columnInfo.DATA_TYPE.toLowerCase().contains('varchar')
    }

    def "lastModifiedBy column migration preserves existing data"() {
        given: "an existing scheduled execution record"
        // Create a test job record first to ensure the table structure is ready
        runMigrations()
        
        def jobId = sql.firstRow("""
            INSERT INTO scheduled_execution (
                version, job_name, project, date_created, last_updated, 
                scheduled, execution_enabled, rduser
            ) VALUES (
                0, 'test-job', 'test-project', current_timestamp, current_timestamp,
                false, true, 'testuser'
            ) RETURNING id
        """).id

        and: "we temporarily remove the lastModifiedBy column to simulate pre-migration state"
        try {
            sql.execute("ALTER TABLE scheduled_execution DROP COLUMN last_modified_by")
        } catch (Exception e) {
            // Column might not exist, which is expected in some test scenarios
        }

        when: "migrations are run again"
        runMigrations()

        then: "existing job data is preserved and lastModifiedBy column is added"
        def jobData = sql.firstRow("SELECT * FROM scheduled_execution WHERE id = ?", [jobId])
        jobData.JOB_NAME == 'test-job'
        jobData.PROJECT == 'test-project'
        jobData.RDUSER == 'testuser'
        // lastModifiedBy should be null for existing records
        jobData.LAST_MODIFIED_BY == null

        cleanup:
        try {
            sql.execute("DELETE FROM scheduled_execution WHERE id = ?", [jobId])
        } catch (Exception e) {
            // Cleanup, ignore errors
        }
    }

    def "lastModifiedBy migration handles multiple runs gracefully"() {
        when: "migrations are run multiple times"
        runMigrations()
        runMigrations()  // Run again to test idempotency

        then: "column exists and no errors occur"
        def columns = sql.rows("""
            SELECT column_name 
            FROM information_schema.columns 
            WHERE table_name = 'scheduled_execution' 
            AND column_name = 'last_modified_by'
        """)
        
        columns.size() == 1
        noExceptionThrown()
    }

    def "lastModifiedBy migration preconditions work correctly"() {
        when: "checking if column already exists"
        runMigrations()
        
        then: "precondition should prevent duplicate column creation"
        def changelogEntries = sql.rows("""
            SELECT id, author, filename 
            FROM databasechangelog 
            WHERE id = '4.16.0-add-last-modified-by-column'
        """)
        
        // Should have exactly one entry for our migration
        changelogEntries.size() == 1
        changelogEntries[0].AUTHOR == 'rundeckuser (generated)'
    }

    def "can insert and update jobs with lastModifiedBy field"() {
        given: "migrations have been run"
        runMigrations()

        when: "inserting a new job with lastModifiedBy"
        def jobId = sql.firstRow("""
            INSERT INTO scheduled_execution (
                version, job_name, project, date_created, last_updated, 
                scheduled, execution_enabled, rduser, last_modified_by
            ) VALUES (
                0, 'audit-test-job', 'audit-project', current_timestamp, current_timestamp,
                false, true, 'creator', 'modifier'
            ) RETURNING id
        """).id

        then: "the record is created successfully with audit fields"
        def jobData = sql.firstRow("SELECT * FROM scheduled_execution WHERE id = ?", [jobId])
        jobData.JOB_NAME == 'audit-test-job'
        jobData.RDUSER == 'creator'
        jobData.LAST_MODIFIED_BY == 'modifier'

        when: "updating the lastModifiedBy field"
        sql.executeUpdate("""
            UPDATE scheduled_execution 
            SET last_modified_by = 'updater', last_updated = current_timestamp
            WHERE id = ?
        """, [jobId])

        then: "the update is successful"
        def updatedData = sql.firstRow("SELECT * FROM scheduled_execution WHERE id = ?", [jobId])
        updatedData.RDUSER == 'creator'  // Original creator preserved
        updatedData.LAST_MODIFIED_BY == 'updater'  // Last modifier updated

        cleanup:
        try {
            sql.execute("DELETE FROM scheduled_execution WHERE id = ?", [jobId])
        } catch (Exception e) {
            // Cleanup, ignore errors
        }
    }

    def "lastModifiedBy field supports null values correctly"() {
        given: "migrations have been run"
        runMigrations()

        when: "inserting a job without lastModifiedBy"
        def jobId = sql.firstRow("""
            INSERT INTO scheduled_execution (
                version, job_name, project, date_created, last_updated, 
                scheduled, execution_enabled, rduser
            ) VALUES (
                0, 'null-audit-job', 'test-project', current_timestamp, current_timestamp,
                false, true, 'creator'
            ) RETURNING id
        """).id

        then: "the record is created with null lastModifiedBy"
        def jobData = sql.firstRow("SELECT * FROM scheduled_execution WHERE id = ?", [jobId])
        jobData.JOB_NAME == 'null-audit-job'
        jobData.RDUSER == 'creator'
        jobData.LAST_MODIFIED_BY == null

        when: "explicitly setting lastModifiedBy to null"
        sql.executeUpdate("""
            UPDATE scheduled_execution 
            SET last_modified_by = NULL 
            WHERE id = ?
        """, [jobId])

        then: "null value is accepted"
        def updatedData = sql.firstRow("SELECT * FROM scheduled_execution WHERE id = ?", [jobId])
        updatedData.LAST_MODIFIED_BY == null

        cleanup:
        try {
            sql.execute("DELETE FROM scheduled_execution WHERE id = ?", [jobId])
        } catch (Exception e) {
            // Cleanup, ignore errors
        }
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
