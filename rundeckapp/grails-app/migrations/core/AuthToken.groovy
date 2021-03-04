databaseChangeLog = {


    changeSet(author: "rundeckuser (generated)", id: "3.4.0-2") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"auth_token")
            }
        }
        createTable(tableName: "auth_token") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "auth_tokenPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: '${timestamp.type}')

            column(name: "uuid", type: '${varchar255.type}')

            column(name: "last_updated", type: '${timestamp.type}')

            column(name: "token", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "expiration", type: '${timestamp.type}')

            column(name: "auth_roles", type: '${text.type}') {
                constraints(nullable: "false")
            }

            column(name: "name", type: '${varchar255.type}')

            column(name: "user_id", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "type", type: '${varchar255.type}')

            column(name: "creator", type: '${varchar255.type}')

            column(name: "token_mode", type: '${varchar255.type}')
        }
    }
}