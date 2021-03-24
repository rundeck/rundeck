databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-11") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"orchestrator")
            }
        }
        createTable(tableName: "orchestrator") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "orchestratorPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "type", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "content", type: '${text.type}')
        }
    }
}