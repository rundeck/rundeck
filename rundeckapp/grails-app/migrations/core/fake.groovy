databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "1231231231231232233") {
//        preConditions(onFail: "MARK_RAN"){
//            not{
//                indexExists (tableName:"zingodingo", indexName: "IDX_ERROR_HANDLER")
//            }
//        }
        createIndex(indexName: "IDX_ERROR_HANDLER", tableName: "zingodingo") {
            column(name: "error_handler_id")
        }
    }
}