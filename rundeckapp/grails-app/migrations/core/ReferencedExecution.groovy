databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-17") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"referenced_execution")
            }
        }
        createTable(tableName: "referenced_execution") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "referenced_executionPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: '${number.type}')

            column(name: "status", type: '${varchar255.type}')

            column(name: "execution_id", type: '${number.type}') {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "rundeckdev", id: "Add-job-uuid-to-refedxec") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "referenced_execution", columnName: 'job_uuid')
            }
        }
        addColumn(tableName: "referenced_execution") {
            column(name: 'job_uuid', type: '${varchar255.type}')
        }

    }
    changeSet(author: "rundeckdev", id: "Populate-refedxec-job-uuid", dbms: "!mssql") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "referenced_execution")
            tableExists(tableName: "scheduled_execution")
        }
        sql("update referenced_execution rex set job_uuid = (select uuid from scheduled_execution where id = rex.scheduled_execution_id)")
    }
    changeSet(author: "rundeckdev", id: "Populate-refedxec-job-uuid-mssql", dbms: "mssql") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "referenced_execution")
            tableExists(tableName: "scheduled_execution")
        }
        sql("update referenced_execution set job_uuid = (select uuid from scheduled_execution where id = referenced_execution.scheduled_execution_id)")
    }
    changeSet(author: "rundeckdev", id: "Index referenced_execution job uuid") {
        preConditions(onFail: "MARK_RAN"){
            not{
                indexExists (tableName:"referenced_execution", indexName: "REFEXEC_IDX_JOBUUID")
            }
        }
        createIndex(indexName: "REFEXEC_IDX_JOBUUID", tableName: "referenced_execution") {
            column(name: "job_uuid")

            column(name: "status")
        }
    }
}