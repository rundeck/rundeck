databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "1600228666999-23") {
        createTable(tableName: "webhook") {
            column(name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "webhook_pkey")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "event_plugin", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "plugin_configuration_json", type: "TEXT") {
                constraints(nullable: "false")
            }

            column(name: "uuid", type: "VARCHAR(255)")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "enabled", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "auth_token", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

}
