databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-23") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"storage")
            }
        }
        createTable(tableName: "storage") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "storagePK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "json_data", type: '${text.type}')

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "dir", type: '${varchar2048.type}')

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "name", type: '${varchar1024.type}') {
                constraints(nullable: "false")
            }

            column(name: "namespace", type: '${varchar255.type}')

            column(name: "data", type: '${bytearray.type}')

            column(name: "path_sha", type: '${varchar40.type}') {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "migrate-storage-data-column-to-longblob-1", dbms: "mysql,mariadb") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "storage")
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE storage MODIFY data longblob;")
            }
            rollback {
            }
        }
    }
}