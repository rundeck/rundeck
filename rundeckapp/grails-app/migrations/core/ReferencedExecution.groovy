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
}