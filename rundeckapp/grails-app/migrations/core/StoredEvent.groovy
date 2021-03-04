databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-24") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"stored_event")
            }
        }
        createTable(tableName: "stored_event") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "stored_eventPK")
            }

            column(name: "schema_version", type: '${int.type}') {
                constraints(nullable: "false")
            }

            column(name: "topic", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "severity", type: '${int.type}') {
                constraints(nullable: "false")
            }

            column(name: "object_id", type: '${varchar64.type}')

            column(name: "sequence", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "meta", type: '${text.type}')

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "server_uuid", type: '${varchar36.type}') {
                constraints(nullable: "false")
            }

            column(name: "subsystem", type: '${varchar128.type}') {
                constraints(nullable: "false")
            }

            column(name: "project_name", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }
        }
    }
}