databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "1619468434682") {
        preConditions(onFail: "MARK_RAN"){
            not{
                indexExists (tableName:"workflow_step", indexName: "IDX_ERROR_HANDLER")
            }
        }
        createIndex(indexName: "IDX_ERROR_HANDLER", tableName: "workflow_step") {
            column(name: "error_handler_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "4.14.0-add-index-error-handler-id"){
        preConditions(onFail: "MARK_RAN"){
            not{
                indexExists(indexName: "workflow_step_error_handler_id_idx", tableName: "workflow_step")
            }
        }

        createIndex(indexName: "workflow_step_error_handler_id_idx", tableName: "workflow_step", unique: false) {
            column(name: "error_handler_id")
        }
    }
}