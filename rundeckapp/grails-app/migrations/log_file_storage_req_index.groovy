/* AUTO_REWORKED_MIGRATION_KEY */
databaseChangeLog = {
    changeSet(author: "rundeck (generated)", id: "1482332598001-8") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(indexName: "execution_id_uniq_1482332597546", schemaName: '${default.schema.name}')
            }
        }
        createIndex(indexName: "execution_id_uniq_1482332597546", tableName: "log_file_storage_request", unique: "true") {
            column(name: "execution_id")
        }
    }
}
