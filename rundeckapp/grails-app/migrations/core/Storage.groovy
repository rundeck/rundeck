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

    final int DATA_FIELD_MAX_SIZE_BYTES = 1000000
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
    changeSet(author: "rundeckuser (generated)", id: "data-col-max-size-to-1MB-1", dbms: "mysql,mariadb,postgresql,h2") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "storage")
        }
        grailsChange{
            change{
                sql.execute("ALTER TABLE storage ADD CONSTRAINT storage_data_col_max_size_1M CHECK (octet_length(data) < " + DATA_FIELD_MAX_SIZE_BYTES + ");")
            }
            rollback{
            }
        }
    }
    changeSet(author: "rundeckuser (generated)", id: "data-col-max-size-to-1MB-1", dbms: "mssql") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "storage")
        }
        grailsChange{
            change{
                sql.execute("ALTER TABLE storage ADD CONSTRAINT storage_data_col_max_size_1M CHECK(DATALENGTH(data) < " + DATA_FIELD_MAX_SIZE_BYTES + ");")
            }
            rollback{
            }
        }
    }
    changeSet(author: "rundeckuser (generated)", id: "data-col-max-size-to-1M-1", dbms: "oracle") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "storage")
        }
        grailsChange{
            change{
                sql.execute("ALTER TABLE storage ADD CONSTRAINT STORAGE_DATA_COL_MAX_SIZE_1M CHECK (lengthb(data) < " + DATA_FIELD_MAX_SIZE_BYTES + ")")
            }
            rollback{
            }
        }
    }
}