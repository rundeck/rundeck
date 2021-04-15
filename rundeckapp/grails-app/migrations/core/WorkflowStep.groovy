databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "workflow_step") {
        preConditions(onFail: "MARK_RAN"){
            not{
                indexExists (tableName:"workflow_step", indexName: "IDX_ERROR_HANDLER")
            }
        }
        createIndex(indexName: "IDX_ERROR_HANDLER", tableName: "workflow_step") {
            column(name: "error_handler_id")
        }
    }
}