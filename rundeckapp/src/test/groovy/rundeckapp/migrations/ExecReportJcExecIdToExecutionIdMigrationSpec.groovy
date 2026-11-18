package rundeckapp.migrations

import liquibase.changelog.ChangeLogParameters
import liquibase.changelog.ChangeSet
import liquibase.changelog.DatabaseChangeLog
import liquibase.change.core.RawSQLChange
import liquibase.resource.ClassLoaderResourceAccessor
import org.grails.config.PropertySourcesConfig
import org.grails.plugins.databasemigration.liquibase.GroovyChangeLogParser
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Verifies the changesets in {@code core/ExecReportJcExecIdToExecutionId.groovy} that copy
 * {@code base_report.jc_exec_id} into {@code base_report.execution_id}.
 *
 * <p>MSSQL databases with a case-sensitive collation (for example {@code SQL_Latin1_General_CP1_CS_AS})
 * only resolve the column by its exact lower-case name, so the MSSQL update must not reference
 * {@code JC_EXEC_ID} in upper case (RUN-5152).</p>
 */
class ExecReportJcExecIdToExecutionIdMigrationSpec extends Specification {

    static final String CHANGELOG = 'core/ExecReportJcExecIdToExecutionId.groovy'
    static final String LEGACY_ID = '1653344096108-52'
    static final String MSSQL_ID = 'RUN-5152-jc-exec-id-to-execution-id-mssql'

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

    /**
     * Returns the SQL of the single raw SQL change in the changeset.
     */
    private static String sqlOf(ChangeSet cs) {
        List<RawSQLChange> changes = cs.changes.findAll { it instanceof RawSQLChange } as List<RawSQLChange>
        assert changes.size() == 1
        changes.first().sql.trim()
    }

    /**
     * Returns the ids of the changesets in this changelog that copy jc_exec_id into execution_id for the given dbms.
     */
    private List<String> copyChangeSetIdsFor(String dbms) {
        changeLog.changeSets.findAll { cs ->
            cs.dbmsSet?.contains(dbms) && cs.changes.any {
                it instanceof RawSQLChange && it.sql.toLowerCase().contains('set execution_id')
            }
        }*.id
    }

    @Unroll
    def "exactly one changeset copies jc_exec_id to execution_id on #dbms"() {
        expect:
        copyChangeSetIdsFor(dbms) == [expectedId]

        where:
        dbms         | expectedId
        'mssql'      | MSSQL_ID
        'h2'         | LEGACY_ID
        'postgresql' | LEGACY_ID
        'mysql'      | '1653344096108-51'
        'mariadb'    | '1653344096108-51'
        'oracle'     | '1653344096108-53'
    }

    def "mssql changeset references the column in lower case so case-sensitive collations resolve it"() {
        when:
        String sql = sqlOf(changeSet(MSSQL_ID))

        then:
        sql == 'update base_report set execution_id = cast(jc_exec_id as bigint)'
        !sql.contains('JC_EXEC_ID')
    }

    def "mssql changeset runs only when both columns exist and is marked as ran otherwise"() {
        when:
        ChangeSet cs = changeSet(MSSQL_ID)

        then:
        cs.dbmsSet == ['mssql'] as Set
        cs.preconditions.onFail.name() == 'MARK_RAN'
        cs.preconditions.nestedPreconditions.size() == 1
        cs.preconditions.nestedPreconditions.first().nestedPreconditions*.columnName as Set == ['execution_id', 'jc_exec_id'] as Set
        cs.preconditions.nestedPreconditions.first().nestedPreconditions*.tableName.unique() == ['base_report']
    }

    def "legacy changeset keeps its original SQL so already-applied checksums stay valid"() {
        when:
        ChangeSet cs = changeSet(LEGACY_ID)

        then:
        cs.dbmsSet == ['h2', 'postgresql'] as Set
        sqlOf(cs) == 'update base_report set execution_id = cast(JC_EXEC_ID as bigint)'
    }

    def "mssql changeset runs after execution_id is added and before jc_exec_id is dropped"() {
        when:
        List<String> ids = changeLog.changeSets*.id

        then:
        ids.indexOf('1653344096108-5') < ids.indexOf(MSSQL_ID)
        ids.indexOf(MSSQL_ID) < ids.indexOf('1653344096108-56')
    }
}
