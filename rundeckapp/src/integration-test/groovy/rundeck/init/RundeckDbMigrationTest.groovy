package rundeck.init

import groovy.sql.Sql
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.beans.factory.annotation.Autowired
import grails.testing.mixin.integration.Integration
import rundeckapp.init.RundeckDbMigration
import spock.lang.AutoCleanup

import javax.sql.DataSource
import spock.lang.Specification

@Integration
@ActiveProfiles('rundeck-test-db')
class RundeckDbMigrationTest extends Specification {

    @Autowired
    DataSource dataSource

    @Autowired
    ApplicationContext applicationContext

    def grailsApplication

    @AutoCleanup
    Sql sql

    File migrationDir



    def setup() {
        String changelogLocation = grailsApplication.config.getProperty("grails.plugin.databasemigration.changelogLocation")
        migrationDir = new File(changelogLocation)
        sql = new Sql(dataSource)
        grailsApplication.config.datSource.driverClassName = 'org.h2.Driver'
        grailsApplication.config.dataSource.url = 'jdbc:h2:mem:testDb'
        def cfg = new ConfigObject()

        cfg.grails.plugin.databasemigration.changelogLocation = migrationDir.absolutePath
        cfg.grails.plugin.databasemigration.changelogFileName = "diff-test-build.groovy"
        cfg.grails.plugin.migration.updateOnStartFileName = 'changelog-test.groovy'
        grailsApplication.config.merge(cfg)

    }

    def cleanupSpec() {
        new File('src/integration-test/resources', 'diff-test-build.groovy').delete()
    }
    void "test dbm gorm diff creates diff file"() {
        given:
        grailsApplication.metadata['build.ident'] = 'test-build'

        when:
        RundeckDbMigration rundeckDbMigration = new RundeckDbMigration()
        rundeckDbMigration.applicationContext = applicationContext
        rundeckDbMigration.grailsApplication = grailsApplication
        rundeckDbMigration.createDiff(migrationDir)

        then:
        new File(migrationDir,'diff-test-build.groovy').exists()

    }

    void "run diff generates database with schema"(){
        given:
        grailsApplication.metadata['build.ident'] = 'test-build'

        when:
        RundeckDbMigration rundeckDbMigration = new RundeckDbMigration()
        rundeckDbMigration.applicationContext = applicationContext
        rundeckDbMigration.grailsApplication = grailsApplication
        rundeckDbMigration.runDiff()

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

    void "rollback empties database"(){
        given:
        grailsApplication.metadata['build.ident'] = 'test-build'

        when:
        String tagName = 'test-build'
        RundeckDbMigration rundeckDbMigration = new RundeckDbMigration()
        rundeckDbMigration.applicationContext = applicationContext
        rundeckDbMigration.grailsApplication = grailsApplication
        rundeckDbMigration.rollback(migrationDir, tagName)

        then:
        sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG').num == 1
        sql.rows('SELECT tag FROM DATABASECHANGELOG')[0].tag == 'test-build'


    }

    void "dbsync runs changelog and executes updateOnStartFilename"(){
        given:
        grailsApplication.metadata['build.ident'] = 'test-build'

        when:
        def cfg = new ConfigObject()
        cfg.grails.plugin.databasemigration.changelogLocation = 'src/integration-test/resources'
        cfg.grails.plugin.migration.updateOnStart = true
        grailsApplication.config.merge(cfg)
        RundeckDbMigration rundeckDbMigration = new RundeckDbMigration()
        rundeckDbMigration.applicationContext = applicationContext
        rundeckDbMigration.grailsApplication = grailsApplication
        rundeckDbMigration.syncDatabase(new File('src/integration-test/resources'))

        then:
        sql.firstRow('SELECT COUNT(*) AS num FROM DATABASECHANGELOG WHERE id=?;', 'create-test-table').num == 1

    }

}