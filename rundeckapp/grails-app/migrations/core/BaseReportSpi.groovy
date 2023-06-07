databaseChangeLog = {
    changeSet(author: "rundeckdev", id: "Add-job-uuid-to-base-report") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "base_report", columnName: 'job_uuid')
            }
        }
        addColumn(tableName: "base_report") {
            column(name: 'job_uuid', type: '${varchar255.type}')
        }
    }

    changeSet(author: "rundeckdev", id: "Populate-base-report-job-uuid") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "base_report")
            tableExists(tableName: "execution")
            tableExists(tableName: "scheduled_execution")
            columnExists(tableName: "base_report", columnName: 'execution_id')

        }
        sql("update base_report\n" +
                "set job_uuid =\n" +
                "        (select scheduled_execution.uuid\n" +
                "         from scheduled_execution\n" +
                "                  inner join execution ON execution.scheduled_execution_id = scheduled_execution.id\n" +
                "         WHERE base_report.execution_id = execution.id)")
    }
}