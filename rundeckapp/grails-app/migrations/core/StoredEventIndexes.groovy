databaseChangeLog = {
    changeSet(author: "gschueler", id: "5.13.0-1747766668933") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(tableName: "stored_event", indexName: "STORED_EVENT_IDX_PROJ_SUBSYS_TOPIC")
            }
        }
        createIndex(indexName: "STORED_EVENT_IDX_PROJ_SUBSYS_TOPIC", tableName: "stored_event") {
            column(name: "project_name")
            column(name: "subsystem")
            column(name: "topic")
        }
    }

    changeSet(author: "gschueler", id: "5.13.0-1747766898542") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(tableName: "stored_event", indexName: "STORED_EVENT_IDX_PROJ_SUBSYS_TOPIC_LASTUPDATED")
            }
        }
        createIndex(indexName: "STORED_EVENT_IDX_PROJ_SUBSYS_TOPIC_LASTUPDATED", tableName: "stored_event") {
            column(name: "project_name")
            column(name: "subsystem")
            column(name: "topic")
            column(name: "last_updated")
        }
    }

}