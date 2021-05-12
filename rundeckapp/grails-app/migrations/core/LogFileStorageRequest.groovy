databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-8") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"log_file_storage_request")
            }
        }

        createTable(tableName: "log_file_storage_request") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "log_file_storage_requestPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "plugin_name", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "filetype", type: '${varchar255.type}')

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "completed", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "execution_id", type: '${number.type}') {
                constraints(nullable: "false")
            }
        }
    }
}