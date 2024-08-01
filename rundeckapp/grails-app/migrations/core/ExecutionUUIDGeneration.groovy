package core

databaseChangeLog = {

    property name: "uuid_function", value: "gen_random_uuid()", dbms: "postgresql"
    property name: "uuid_function", value: "uuid()", dbms: "mysql,mariadb"
    property name: "uuid_function", value: "newid()", dbms: "mssql"
    property name: "uuid_function", value: "RANDOM_UUID()", dbms: "h2"
    property name: "uuid_function", value: "sys_guid()", dbms: "oracle"

    changeSet(author: "ahormazabal (generated)", id: "gen-exec-uuids-20240801-00") {
        comment { 'Populate UUID into execution table' }
        preConditions(onFail: 'MARK_RAN') {
            and {
                columnExists(tableName: "execution", columnName: 'uuid')
                columnExists(tableName: "execution", columnName: 'job_uuid')
            }
        }

        update(tableName: "execution") {
            column(name: "uuid", valueComputed: '${uuid_function}')
            column(name: "job_uuid", valueComputed: "SELECT s.uuid FROM scheduled_execution s WHERE s.id = scheduled_execution_id")
            where("uuid IS NULL")
        }
    }


    changeSet(author: "ahormazabal (generated)", id: "gen-exec-uuids-basereport-20240801-01") {
        comment { 'Populate UUID into base_report table' }
        preConditions(onFail: 'MARK_RAN') {
            columnExists(tableName: "base_report", columnName: 'execution_uuid')
        }

        update(tableName: "base_report") {
            column(name: "execution_uuid", valueComputed: 'SELECT e.uuid FROM execution e WHERE execution_id = e.id')
            where("execution_uuid IS NULL")
        }
    }


}
