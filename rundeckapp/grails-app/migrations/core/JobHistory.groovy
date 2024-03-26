databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "5.2.0-job-history-creation") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"job_history")
            }
        }
        createTable(tableName: "job_history") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "job_history_PK")
            }

            column(name: "job_uuid", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "job_definition", type: '${varchar255.type}')

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

        }
    }
 }