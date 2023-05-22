databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-3") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"base_report")
            }
        }
        createTable(tableName: "base_report") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "base_reportPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "tags", type: '${varchar255.type}')

            column(name: "ctx_project", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "node", type: '${varchar255.type}')

            column(name: "ctx_type", type: '${varchar255.type}')

            column(name: "author", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "title", type: '${text.type}') {
                constraints(nullable: "false")
            }

            column(name: "message", type: '${text.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_completed", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_started", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "status", type: '${varchar256.type}') {
                constraints(nullable: "false")
            }

            column(name: "action_type", type: '${varchar256.type}') {
                constraints(nullable: "false")
            }

            column(name: "mapref_uri", type: '${varchar255.type}')

            column(name: "report_id", type: '${varchar3072.type}')

            column(name: "ctx_name", type: '${varchar255.type}')

            column(name: "class", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "aborted_by_user", type: '${varchar255.type}')

            column(name: "ctx_controller", type: '${varchar255.type}')

            column(name: "succeeded_node_list", type: '${text.type}')

            column(name: "ctx_command", type: '${varchar255.type}')

            column(name: "jc_exec_id", type: '${varchar255.type}')

            column(name: "jc_job_id", type: '${varchar255.type}')

            column(name: "failed_node_list", type: '${text.type}')

            column(name: "adhoc_execution", type: '${boolean.type}')

            column(name: "adhoc_script", type: '${text.type}')

            column(name: "filter_applied", type: '${text.type}')
        }
    }
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
        changeSet(author: "rundeckdev", id: "Populate-base-report-job-uuid", dbms: "!mssql") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "base_report")
            tableExists(tableName: "execution")
            tableExists(tableName: "scheduled_execution")
        }
        sql("update base_report br\n" +
                "set job_uuid =\n" +
                "        (select se.uuid\n" +
                "         from scheduled_execution se\n" +
                "                  inner join execution ex ON ex.scheduled_execution_id = se.id\n" +
                "         WHERE br.execution_id = ex.id)")
    }

    changeSet(author: "rundeckdev", id: "Populate-base-report-job-uuid-mssql", dbms: "mssql") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "base_report")
            tableExists(tableName: "execution")
            tableExists(tableName: "scheduled_execution")
        }
        sql("update base_report\n" +
                "set job_uuid =\n" +
                "        (select scheduled_execution.uuid\n" +
                "         from scheduled_execution\n" +
                "                  inner join execution ON execution.scheduled_execution_id = scheduled_execution.id\n" +
                "         WHERE base_report.execution_id = execution.id)")
    }
}