databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-19") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"scheduled_execution")
            }
        }

        createTable(tableName: "scheduled_execution") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "scheduled_executionPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "log_output_threshold", type: '${varchar256.type}')

            column(name: "do_nodedispatch", type: '${boolean.type}')

            column(name: "next_execution", type: '${timestamp.type}')

            column(name: "date_created", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "node_keepgoing", type: '${boolean.type}')

            column(name: "node_exclude_os_arch", type: '${text.type}')

            column(name: "uuid", type: '${varchar255.type}')

            column(name: "node_include", type: '${text.type}')

            column(name: "success_on_empty_node_filter", type: '${boolean.type}')

            column(name: "node_exclude_os_version", type: '${text.type}')

            column(name: "timeout", type: '${text.type}')

            column(name: "node_exclude_precedence", type: '${boolean.type}')

            column(name: "node_exclude_name", type: '${text.type}')

            column(name: "notify_avg_duration_threshold", type: '${text.type}')

            column(name: "day_of_week", type: '${varchar255.type}')

            column(name: "node_include_os_version", type: '${text.type}')

            column(name: "node_exclude_os_name", type: '${text.type}')

            column(name: "retry", type: '${text.type}')

            column(name: "filter", type: '${text.type}')

            column(name: "group_path", type: '${varchar2048.type}')

            column(name: "scheduled", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "orchestrator_id", type: '${number.type}')

            column(name: "node_threadcount_dynamic", type: '${varchar255.type}')

            column(name: "node_include_name", type: '${text.type}')

            column(name: "multiple_executions", type: '${boolean.type}')

            column(name: "time_zone", type: '${varchar256.type}')

            column(name: "rduser", type: '${varchar255.type}')

            column(name: "node_include_os_name", type: '${text.type}')

            column(name: "filter_exclude", type: '${text.type}')

            column(name: "node_exclude", type: '${text.type}')

            column(name: "node_rank_order_ascending", type: '${boolean.type}')

            column(name: "nodes_selected_by_default", type: '${boolean.type}')

            column(name: "node_include_os_arch", type: '${text.type}')

            column(name: "loglevel", type: '${varchar255.type}')

            column(name: "node_exclude_os_family", type: '${text.type}')

            column(name: "execution_enabled", type: '${boolean.type}')

            column(name: "max_multiple_executions", type: '${varchar256.type}')

            column(name: "node_include_os_family", type: '${text.type}')

            column(name: "last_updated", type: '${timestamp.type}') {
                constraints(nullable: "false")
            }

            column(name: "retry_delay", type: '${text.type}')

            column(name: "workflow_id", type: '${number.type}')

            column(name: "exec_count", type: '${number.type}')

            column(name: "month", type: '${varchar255.type}')

            column(name: "hour", type: '${varchar255.type}')

            column(name: "log_output_threshold_action", type: '${varchar256.type}')

            column(name: "arg_string", type: '${text.type}')

            column(name: "user_role_list", type: '${text.type}')

            column(name: "total_time", type: '${number.type}')

            column(name: "node_rank_attribute", type: '${varchar255.type}')

            column(name: "server_nodeuuid", type: '${varchar36.type}')

            column(name: "default_tab", type: '${varchar256.type}')

            column(name: "node_exclude_tags", type: '${text.type}')

            column(name: "seconds", type: '${varchar255.type}')

            column(name: "exclude_filter_uncheck", type: '${boolean.type}')

            column(name: "ref_exec_count", type: '${number.type}')

            column(name: "node_threadcount", type: '${int.type}')

            column(name: "node_include_tags", type: '${text.type}')

            column(name: "job_name", type: '${varchar1024.type}') {
                constraints(nullable: "false")
            }

            column(name: "schedule_enabled", type: '${boolean.type}')

            column(name: "year", type: '${varchar255.type}')

            column(name: "day_of_month", type: '${varchar255.type}')

            column(name: "node_filter_editable", type: '${boolean.type}')

            column(name: "log_output_threshold_status", type: '${varchar256.type}')

            column(name: "description", type: '${text.type}')

            column(name: "minute", type: '${varchar255.type}')

            column(name: "project", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "plugin_config", type: '${text.type}')
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-20") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"scheduled_execution_filter")
            }
        }
        createTable(tableName: "scheduled_execution_filter") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "scheduled_execution_filterPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "loglevel_filter", type: '${varchar255.type}')

            column(name: "group_path", type: '${varchar255.type}')

            column(name: "server_nodeuuidfilter", type: '${varchar255.type}')

            column(name: "name", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "idlist", type: '${varchar255.type}')

            column(name: "user_id", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "scheduled_filter", type: '${varchar255.type}')

            column(name: "proj_filter", type: '${varchar255.type}')

            column(name: "desc_filter", type: '${varchar255.type}')

            column(name: "job_filter", type: '${varchar255.type}')
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-21") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"scheduled_execution_stats")
            }
        }
        createTable(tableName: "scheduled_execution_stats") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "scheduled_execution_statsPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "se_id", type: '${number.type}')

            column(name: "content", type: '${text.type}') {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "3.4.0-22", dbms: "h2") {
        comment { 'rename filter to "FILTER' }
        preConditions(onFail: 'CONTINUE') {
            grailsPrecondition {
                check {
                    def ran = sql.firstRow("SELECT count(*) as num FROM INFORMATION_SCHEMA.columns where table_name ='SCHEDULED_EXECUTION' and column_name  = 'filter'").num

                    if(ran==0) fail('precondition is not satisfied')
                }
            }
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE scheduled_execution RENAME COLUMN \"filter\" TO FILTER;")
            }
            rollback {
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "4.6.0-1", dbms: "h2") {
        comment { 'rename month to MONTH' }
        preConditions(onFail: "CONTINUE"){
            columnExists(tableName: "scheduled_execution", columnName: "month")
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE scheduled_execution RENAME COLUMN \"month\" TO MONTH;")
            }
            rollback {
            }
        }
    }
    
    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "4.6.0-2", dbms: "h2") {
        comment { 'rename hour to HOUR' }
        preConditions(onFail: "CONTINUE"){
            columnExists(tableName: "scheduled_execution", columnName: "hour")
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE scheduled_execution RENAME COLUMN \"hour\" TO HOUR;")
            }
            rollback {
            }
        }
    }
    
    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "4.6.0-3", dbms: "h2") {
        comment { 'rename year to YEAR' }
        preConditions(onFail: "CONTINUE"){
            columnExists(tableName: "scheduled_execution", columnName: "year")
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE scheduled_execution RENAME COLUMN \"year\" TO YEAR;")
            }
            rollback {
            }
        }
    }
    
    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "4.6.0-4", dbms: "h2") {
        comment { 'rename minute to MINUTE' }
        preConditions(onFail: "CONTINUE"){
            columnExists(tableName: "scheduled_execution", columnName: "minute")
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE scheduled_execution RENAME COLUMN \"minute\" TO MINUTE;")
            }
            rollback {
            }
        }
    }


    changeSet(author: "rundeckdev", id: "4.11.0-add-job-uuid-to-stats-mssql", dbms: "mssql") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "scheduled_execution_stats", columnName: 'job_uuid')
            }
        }
        addColumn(tableName: "scheduled_execution_stats") {
            column(name: 'job_uuid', type: '${varchar255.type}')
        }
        sql("update scheduled_execution_stats set job_uuid = (select uuid from scheduled_execution where id = scheduled_execution_stats.se_id)")
    }

    changeSet(author: "rundeckdev", id: "4.11.0-add-job-uuid-to-stats", dbms: "!mssql") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "scheduled_execution_stats", columnName: 'job_uuid')
            }
        }
        addColumn(tableName: "scheduled_execution_stats") {
            column(name: 'job_uuid', type: '${varchar255.type}')
        }
        sql("update scheduled_execution_stats stats set job_uuid = (select uuid from scheduled_execution where id = stats.se_id)")
    }

    changeSet(author: "rundeckuser (generated)", id: "4.14.0-add-index-workflow-id"){
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(indexName: "scheduled_execution_workflow_id_idx", tableName: "scheduled_execution")
            }
        }

        createIndex(indexName: "scheduled_execution_workflow_id_idx", tableName: "scheduled_execution", unique: false) {
            column(name: "workflow_id")
        }
    }


    changeSet(author: "rundeckuser (generated)", id: "5.0-job-filter-1001") {
        comment { 'remove scheduled_execution_filter table' }
        preConditions(onFail: "MARK_RAN"){
            tableExists (tableName:"scheduled_execution_filter")
        }
        dropTable(tableName: "scheduled_execution_filter")
    }
}
