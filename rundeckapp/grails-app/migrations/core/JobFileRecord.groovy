databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-6") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"job_file_record")
            }
        }

        createTable(tableName: "job_file_record") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "job_file_recordPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "uuid", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "SIZE", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "record_name", type: '${varchar255.type}')

            column(name: "storage_reference", type: '${text.type}') {
                constraints(nullable: "false")
            }

            column(name: "file_state", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "file_name", type: '${varchar1024.type}')

            column(name: "storage_type", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "storage_meta", type: '${text.type}')

            column(name: "record_type", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "execution_id", type: '${number.type}')

            column(name: "server_nodeuuid", type: '${varchar255.type}')

            column(name: "rduser", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "job_id", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "sha", type: '${varchar64.type}') {
                constraints(nullable: "false")
            }

            column(name: "project", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "expiration_date", type: '${timestamp.type}')
        }
    }

    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "3.4.0-7", dbms: "postgresql") {
        comment { 'rename size to "SIZE' }
        preConditions(onFail: 'MARK_RAN') {
            grailsPrecondition {
                check {
                    def ran = sql.firstRow("SELECT COUNT(*) AS num FROM INFORMATION_SCHEMA.columns where table_name = 'job_file_record' and column_name = 'size'").num
                    if(ran==0) fail('precondition is not satisfied')
                }
            }
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE job_file_record RENAME COLUMN size TO \"SIZE\";")
            }
            rollback {
            }
        }
    }
}