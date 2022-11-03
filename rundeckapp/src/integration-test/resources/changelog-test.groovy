databaseChangeLog = {

    changeSet(author: "Rundeck Tester", id: "create-test-table") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"integrationTestTable")
            }
        }
        createTable(tableName: "integrationTestTable") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(primaryKey: "true", primaryKeyName: "testPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "description", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

}
