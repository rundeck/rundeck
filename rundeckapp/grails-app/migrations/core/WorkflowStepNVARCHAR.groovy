databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "1637595458", dbms: "mssql") {
        comment { 'change column type to nvarchar for description and adhoc_remote_string' }
        preConditions(onFail: 'MARK_RAN') {
            grailsPrecondition {
                check {
                    def ran = sql.firstRow("SELECT count(*) as num FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'workflow_step' AND (COLUMN_NAME = 'description' AND DATA_TYPE = 'varchar') OR (COLUMN_NAME = 'adhoc_remote_string' AND DATA_TYPE = 'varchar')").num
                    if (ran == 0) fail('precondition is not satisfied')
                }
            }
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE \"workflow_step\" ALTER COLUMN \"description\" nvarchar(max) null;")
                sql.execute("ALTER TABLE \"workflow_step\" ALTER COLUMN \"adhoc_remote_string\" nvarchar(max) null;")
            }
            rollback {
            }
        }
    }
}