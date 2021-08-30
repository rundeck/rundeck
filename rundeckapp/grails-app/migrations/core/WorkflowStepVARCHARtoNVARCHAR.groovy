databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "1630329719623", dbms: "mssql") {
        comment { 'change column type to nvarchar' }
        preConditions(onFail: 'MARK_RAN') {
            grailsPrecondition {
                check {
                    def ran = sql.firstRow("SELECT count(*) as num FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'workflow_step' AND COLUMN_NAME = 'adhoc_local_string' AND DATA_TYPE = 'varchar'").num
                    if (ran == 0) fail('precondition is not satisfied')
                }
            }
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE \"workflow_step\" ALTER COLUMN \"adhoc_local_string\" nvarchar(max) null;")
            }
            rollback {
            }
        }
    }
}