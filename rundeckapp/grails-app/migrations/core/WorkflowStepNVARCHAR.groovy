databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "1637595458", dbms: "mssql") {
        comment { 'change column type to nvarchar for description and adhoc_remote_string' }
        preConditions(onFail: 'MARK_RAN') {
            grailsPrecondition {
                check {
                    def ran = sql.firstRow("SELECT count(*) as num FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME COLLATE Latin1_General_CI_AS = 'workflow_step' AND ((COLUMN_NAME COLLATE Latin1_General_CI_AS = 'description' AND DATA_TYPE COLLATE Latin1_General_CI_AS = 'varchar') OR (COLUMN_NAME COLLATE Latin1_General_CI_AS = 'adhoc_remote_string' AND DATA_TYPE COLLATE Latin1_General_CI_AS = 'varchar'))").num
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