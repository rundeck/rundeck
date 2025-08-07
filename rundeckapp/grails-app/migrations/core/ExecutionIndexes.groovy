databaseChangeLog = {
    changeSet(author: "cfranco", id: "5.15.0-1754578206") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(tableName: "execution", indexName: "EXECUTION_RETRY_EXEC_ID_IDX")
            }
        }
        createIndex(indexName: "EXECUTION_RETRY_EXEC_ID_IDX", tableName: "execution") {
            column(name: "retry_execution_id")
        }
    }

}