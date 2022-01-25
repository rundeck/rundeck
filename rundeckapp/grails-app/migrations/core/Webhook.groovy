databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-25") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"webhook")
            }
        }
        createTable(tableName: "webhook") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "webhookPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "event_plugin", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "plugin_configuration_json", type: '${text.type}') {
                constraints(nullable: "false")
            }

            column(name: "uuid", type: '${varchar255.type}')

            column(name: "name", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "enabled", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "project", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "auth_token", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "Stephen Joyner", id: "3.4.11-webhook-auth_config_json") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "webhook", columnName: 'auth_config_json')
            }
        }
        addColumn(tableName: "webhook") {
            column(name: 'auth_config_json', type: '${text.type}')
        }
    }
}