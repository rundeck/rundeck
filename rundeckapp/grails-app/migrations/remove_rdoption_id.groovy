/* AUTO_REWORKED_MIGRATION_KEY */
databaseChangeLog = {
    changeSet(author: "rundeck (generated)", id: "1482332598001-9") {
        preConditions(onFail: "MARK_RAN") {
            columnExists(columnName: "id", tableName: "rdoption_values", schemaName: '${default.schema.name}')
        }
        dropColumn(columnName: "id", tableName: "rdoption_values")
    }
}
