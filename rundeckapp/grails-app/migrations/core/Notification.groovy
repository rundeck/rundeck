databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-10") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"notification")
            }
        }
        createTable(tableName: "notification") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "notificationPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "event_trigger", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "type", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "content", type: '${text.type}')

            column(name: "format", type: '${varchar255.type}')
        }
    }
}