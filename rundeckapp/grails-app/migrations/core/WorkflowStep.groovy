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

    changeSet(author: "greg (generated)", id: "1620252002774-1") {
        preConditions(onFail: "MARK_RAN"){
            not{
                columnExists (tableName:"workflow_step", columnName: "preserve_quotes")
            }
        }
        addColumn(tableName: "workflow_step") {
            column(name: "preserve_quotes", type: '${boolean.type}')
        }
    }
}
