databaseChangeLog = {
    changeSet(author: "cfranco", id: "5.15.0-1754578206", dbms: "oracle,postgresql") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(tableName: "execution", indexName: "EXECUTION_RETRY_EXEC_ID_IDX")
            }
        }
        createIndex(indexName: "EXECUTION_RETRY_EXEC_ID_IDX", tableName: "execution") {
            column(name: "retry_execution_id")
        }
    }

    // Misfire recovery counts executions per job within a date window
    // (countByScheduledExecutionAndDateStartedBetween). No existing index covers
    // (scheduled_execution_id, date_started), and the MySQL 8.0 optimizer picks a
    // plan that can exceed the client socket timeout on large execution tables.
    changeSet(author: "lalamo", id: "5.15.0-add-sched-date-started-index") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(tableName: "execution", indexName: "EXEC_IDX_SCHED_DATE_STARTED")
            }
        }
        createIndex(indexName: "EXEC_IDX_SCHED_DATE_STARTED", tableName: "execution", unique: false) {
            column(name: "scheduled_execution_id")
            column(name: "date_started")
        }
    }

}