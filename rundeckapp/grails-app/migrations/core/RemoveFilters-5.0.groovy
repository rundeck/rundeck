databaseChangeLog = {
    changeSet(author: "gschueler (generated)", failOnError: "false", id: "5.0-nodefilter-0100") {
        comment { 'remove node_filter table' }
        preConditions(onFail: 'MARK_RAN') {
            tableExists(tableName: "node_filter")
        }
        dropTable(tableName: "node_filter") {

        }
    }

    changeSet(author: "gschueler", id: "5.0-nodefilter-0500") {
        preConditions(onFail: "MARK_RAN"){
            foreignKeyConstraintExists (foreignKeyTableName: "node_filter", foreignKeyName: "FKhuj3n3gy2kqoie8d8oucay4b3")
        }
        dropForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "node_filter", constraintName: "FKhuj3n3gy2kqoie8d8oucay4b3", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }

    changeSet(author: "gschueler (generated)", failOnError:"false", id: "5.0-reportfilter-0100") {
        comment { 'remove report_filter table' }
        preConditions(onFail: 'MARK_RAN') {
            tableExists (tableName:"report_filter")
        }
        dropTable(tableName: "report_filter")
    }

    changeSet(author: "rundeckuser (generated)", id: "5.0-reportfilter-0500") {
        preConditions(onFail: "MARK_RAN"){
            foreignKeyConstraintExists (foreignKeyTableName: "report_filter", foreignKeyName: "FKcdly7hl8164nfb0e5908ch64n")
        }
        dropForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "report_filter", constraintName: "FKcdly7hl8164nfb0e5908ch64n", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }
    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "5.0-reportfilter-0600", dbms:"mysql,postgresql,mariadb,oracle,h2") {
        preConditions(onFail: "MARK_RAN"){
            uniqueConstraintExists (catalogName: '${catalogName}', tableName:"report_filter", constraintName:"UC_REPORT_FILTERNAME_COL")
        }
        dropUniqueConstraint(columnNames: "name", constraintName: "UC_REPORT_FILTERNAME_COL", tableName: "report_filter")
    }
    changeSet(author: "rundeckuser (generated)", id: "5.0-reportfilter-0700", dbms:"mssql"){
        preConditions(onFail: "MARK_RAN"){
            uniqueConstraintExists(columnNames: "name", constraintName: "UC_REPORT_FILTERNAME_COL", tableName: "report_filter")
        }
        dropUniqueConstraint(columnNames: "name", constraintName: "UC_REPORT_FILTERNAME_COL", tableName: "report_filter")
    }

    changeSet(author: "rundeckuser (generated)", id: "5.0-job-filter-1001") {
        comment { 'remove scheduled_execution_filter table' }
        preConditions(onFail: "MARK_RAN"){
            tableExists (tableName:"scheduled_execution_filter")
        }
        dropTable(tableName: "scheduled_execution_filter")
    }
    changeSet(author: "gschueler", id: "5.0-jobfilter-0501") {
        preConditions(onFail: "MARK_RAN"){
            foreignKeyConstraintExists (foreignKeyTableName: "scheduled_execution_filter", foreignKeyName: "FK22545y15qs4iqod1ljyqsm1fi")
        }
        dropForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "scheduled_execution_filter", constraintName: "FK22545y15qs4iqod1ljyqsm1fi", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }
}