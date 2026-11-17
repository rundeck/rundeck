package rundeckapp.migrations

import liquibase.change.core.RawSQLChange
import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.precondition.core.SqlPrecondition
import liquibase.resource.ClassLoaderResourceAccessor
import org.grails.config.PropertySourcesConfig
import org.grails.plugins.databasemigration.liquibase.GroovyChangeLogParser
import spock.lang.Shared
import spock.lang.Specification

/**
 * Verifies the changesets in {@code core/RDUSER.groovy} for the {@code rduser.password} column.
 *
 * <p>The table is created with an upper-case {@code PASSWORD} column while Hibernate queries
 * {@code password}. MSSQL databases with a case-sensitive collation (for example
 * {@code SQL_Latin1_General_CP1_CS_AS}) cannot resolve it, so login fails (RUN-5152).</p>
 */
class RduserPasswordColumnMigrationSpec extends Specification {

    static final String CHANGELOG = 'core/RDUSER.groovy'
    static final String CREATE_ID = '3.4.0-16'
    static final String MSSQL_ID = 'RUN-5152-rduser-password-lower-case-mssql'

    @Shared
    DatabaseChangeLog changeLog

    def setupSpec() {
        GroovyChangeLogParser parser = new GroovyChangeLogParser()
        parser.config = new PropertySourcesConfig()
        changeLog = parser.parse(CHANGELOG, new ChangeLogParameters(), new ClassLoaderResourceAccessor())
    }

    /**
     * Returns the changeset with the given id, failing when it is missing.
     */
    private ChangeSet changeSet(String id) {
        ChangeSet found = changeLog.changeSets.find { it.id == id }
        assert found != null: "changeset ${id} not found in ${CHANGELOG}"
        found
    }

    def "mssql changeset renames rduser.PASSWORD to lower case"() {
        when:
        ChangeSet cs = changeSet(MSSQL_ID)
        List<RawSQLChange> changes = cs.changes.findAll { it instanceof RawSQLChange } as List<RawSQLChange>

        then:
        cs.dbmsSet == ['mssql'] as Set
        changes.size() == 1
        changes.first().sql.trim() == "EXEC sp_rename 'rduser.PASSWORD', 'password', 'COLUMN'"
    }

    def "mssql changeset runs only while the column is still upper case and is marked as ran otherwise"() {
        when:
        ChangeSet cs = changeSet(MSSQL_ID)
        SqlPrecondition check = cs.preconditions.nestedPreconditions.first() as SqlPrecondition

        then:
        cs.preconditions.onFail.name() == 'MARK_RAN'
        cs.preconditions.nestedPreconditions.size() == 1
        check.expectedResult == '1'
        check.sql.contains("OBJECT_ID('rduser')")
        check.sql.contains("COLLATE Latin1_General_BIN = 'PASSWORD'")
    }

    def "mssql changeset runs after the rduser table is created"() {
        when:
        List<String> ids = changeLog.changeSets*.id

        then:
        ids.indexOf(CREATE_ID) < ids.indexOf(MSSQL_ID)
    }

    def "create table changeset keeps the original column name so already-applied checksums stay valid"() {
        when:
        ChangeSet cs = changeSet(CREATE_ID)

        then:
        cs.changes.first().columns*.name.contains('PASSWORD')
    }
}
