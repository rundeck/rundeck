/* AUTO_REWORKED_MIGRATION_KEY */
databaseChangeLog = {
    changeSet(author: "rundeck (generated)", id: "1482332598001-1") {
        modifyDataType(columnName: "failed_node_list", newDataType: '${text.type}', tableName: "base_report")
    }

    changeSet(author: "rundeck (generated)", id: "1482332598001-2") {
        modifyDataType(columnName: "filter_applied", newDataType: '${text.type}', tableName: "base_report")
    }

    changeSet(author: "rundeck (generated)", id: "1482332598001-3") {
        modifyDataType(columnName: "succeeded_node_list", newDataType: '${text.type}', tableName: "base_report")
    }

    changeSet(author: "rundeck (generated)", id: "1482332598001-4") {
        modifyDataType(columnName: "json_data", newDataType: '${text.type}', tableName: "plugin_meta")
    }
}
