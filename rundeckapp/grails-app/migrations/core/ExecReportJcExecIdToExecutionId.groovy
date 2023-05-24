package core

databaseChangeLog = {

    changeSet(author: "gschueler (generated)", id: "1653344096108-5") {
        comment{'Convert jc_exec_id varchar to execution_id bigint'}
        preConditions(onFail: 'MARK_RAN') {
            not {
                columnExists(tableName: "base_report", columnName: 'execution_id')
            }
        }
        addColumn(tableName: "base_report") {
            column(name: "execution_id", type: '${number.type}')
        }
    }

    changeSet(author: "gschueler (generated)", id: "1653344096108-51", dbms: 'mysql,mariadb') {
        preConditions(onFail: 'MARK_RAN') {
            and {
                columnExists(tableName: "base_report", columnName: 'execution_id')
                columnExists(tableName: "base_report", columnName: 'jc_exec_id')
            }
        }
        sql("""update base_report set execution_id = (IF(jc_exec_id REGEXP '^[0-9]+\$', CAST(jc_exec_id as unsigned),
 null))""")
    }

    changeSet(author: "gschueler (generated)", id: "1653344096108-52", dbms: 'h2,postgresql,mssql') {
        preConditions(onFail: 'MARK_RAN') {
            and {
                columnExists(tableName: "base_report", columnName: 'execution_id')
                columnExists(tableName: "base_report", columnName: 'jc_exec_id')
            }
        }
        sql("""update base_report set execution_id = cast(JC_EXEC_ID as bigint)""")
    }

    changeSet(author: "gschueler (generated)", id: "1653344096108-53", dbms: 'oracle') {
        preConditions(onFail: 'MARK_RAN') {
            and {
                columnExists(tableName: "base_report", columnName: 'execution_id')
                columnExists(tableName: "base_report", columnName: 'jc_exec_id')
            }
        }
        sql("""update base_report set execution_id = CAST(JC_EXEC_ID as NUMBER(19, 0))""")
    }

    changeSet(author: "gschueler (generated)", id: "1653344096108-54") {
        preConditions(onFail: "MARK_RAN"){
            indexExists (tableName:"base_report", indexName: "EXEC_REPORT_IDX_0")
        }
        dropIndex(indexName: "EXEC_REPORT_IDX_0", tableName: "base_report")
    }
    changeSet(author: "gschueler (generated)", id: "1653344096108-55") {
        preConditions(onFail: "MARK_RAN"){
            indexExists (tableName:"base_report", indexName: "EXEC_REPORT_IDX_2")
        }
        dropIndex(indexName: "EXEC_REPORT_IDX_2", tableName: "base_report")
    }

    changeSet(author: "gschueler (generated)", id: "1653344096108-56") {
        preConditions(onFail: 'MARK_RAN') {
            columnExists(tableName: "base_report", columnName: 'jc_exec_id')
        }
        dropColumn(columnName: "jc_exec_id", tableName: "base_report")
    }

    changeSet(author: "gschueler (generated)", id: "1653344096108-2") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(tableName: "base_report", indexName: "EXEC_REPORT_IDX_0")
            }
        }
        createIndex(indexName: "EXEC_REPORT_IDX_0", tableName: "base_report", unique: "false") {
            column(name: "ctx_project")

            column(name: "date_completed")

            column(name: "jc_job_id")

            column(name: "execution_id")
        }
    }


    changeSet(author: "gschueler (generated)", id: "1653344096108-4") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(tableName: "base_report", indexName: "EXEC_REPORT_IDX_2")
            }
        }
        createIndex(indexName: "EXEC_REPORT_IDX_2", tableName: "base_report", unique: "false") {
            column(name: "execution_id")
        }
    }
}
