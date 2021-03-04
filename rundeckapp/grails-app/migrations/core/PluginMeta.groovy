databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-12") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"plugin_meta")
            }
        }
        createTable(tableName: "plugin_meta") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "plugin_metaPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "json_data", type: '${text.type}')

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "data_key", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "project", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }
        }
    }
}