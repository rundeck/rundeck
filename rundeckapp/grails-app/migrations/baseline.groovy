/* AUTO_REWORKED_MIGRATION_KEY */
databaseChangeLog = {
    changeSet(author: "rundeck (generated)", id: "1482332299354-1") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "auth_token")
            }
        }
        createTable(tableName: "auth_token") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "auth_roles", type: '${text.type}') {
                constraints(nullable: "false")
            }

            column(name: "token", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-2") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "base_report")
            }
        }
        createTable(tableName: "base_report") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "action_type", type: "VARCHAR(256)") {
                constraints(nullable: "false")
            }

            column(name: "author", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "ctx_name", type: "VARCHAR(255)")

            column(name: "ctx_project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "ctx_type", type: "VARCHAR(255)")

            column(name: "date_completed", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "date_started", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "mapref_uri", type: "VARCHAR(255)")

            column(name: "message", type: '${text.type}') {
                constraints(nullable: "false")
            }

            column(name: "node", type: "VARCHAR(255)")

            column(name: "report_id", type: "VARCHAR(3072)")

            column(name: "status", type: "VARCHAR(256)") {
                constraints(nullable: "false")
            }

            column(name: "tags", type: "VARCHAR(255)")

            column(name: "title", type: '${text.type}') {
                constraints(nullable: "false")
            }

            column(name: "class", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "aborted_by_user", type: "VARCHAR(255)")

            column(name: "adhoc_execution", type: '${boolean.type}')

            column(name: "adhoc_script", type: '${text.type}')

            column(name: "ctx_command", type: "VARCHAR(255)")

            column(name: "ctx_controller", type: "VARCHAR(255)")

            column(name: "jc_exec_id", type: "VARCHAR(255)")

            column(name: "jc_job_id", type: "VARCHAR(255)")

            column(name: "failed_node_list", type: "VARCHAR(255)")

            column(name: "filter_applied", type: "VARCHAR(255)")

            column(name: "succeeded_node_list", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-3") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "execution")
            }
        }
        createTable(tableName: "execution") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "abortedby", type: "VARCHAR(255)")

            column(name: "arg_string", type: '${text.type}')

            column(name: "cancelled", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_completed", type: "DATETIME")

            column(name: "date_started", type: "DATETIME")

            column(name: "do_nodedispatch", type: '${boolean.type}')

            column(name: "failed_node_list", type: '${text.type}')

            column(name: "filter", type: '${text.type}')

            column(name: "loglevel", type: "VARCHAR(255)")

            column(name: "node_exclude", type: '${text.type}')

            column(name: "node_exclude_name", type: '${text.type}')

            column(name: "node_exclude_os_arch", type: '${text.type}')

            column(name: "node_exclude_os_family", type: '${text.type}')

            column(name: "node_exclude_os_name", type: '${text.type}')

            column(name: "node_exclude_os_version", type: '${text.type}')

            column(name: "node_exclude_precedence", type: '${boolean.type}')

            column(name: "node_exclude_tags", type: '${text.type}')

            column(name: "node_include", type: '${text.type}')

            column(name: "node_include_name", type: '${text.type}')

            column(name: "node_include_os_arch", type: '${text.type}')

            column(name: "node_include_os_family", type: '${text.type}')

            column(name: "node_include_os_name", type: '${text.type}')

            column(name: "node_include_os_version", type: '${text.type}')

            column(name: "node_include_tags", type: '${text.type}')

            column(name: "node_keepgoing", type: '${boolean.type}')

            column(name: "node_rank_attribute", type: "VARCHAR(255)")

            column(name: "node_rank_order_ascending", type: '${boolean.type}')

            column(name: "node_threadcount", type: "INT")

            column(name: "orchestrator_id", type: "BIGINT")

            column(name: "outputfilepath", type: '${text.type}')

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "retry", type: '${text.type}')

            column(name: "retry_attempt", type: "INT")

            column(name: "retry_execution_id", type: "BIGINT")

            column(name: "scheduled_execution_id", type: "BIGINT")

            column(name: "server_nodeuuid", type: "VARCHAR(36)")

            column(name: "status", type: "VARCHAR(255)")

            column(name: "succeeded_node_list", type: '${text.type}')

            column(name: "timed_out", type: '${boolean.type}')

            column(name: "timeout", type: '${text.type}')

            column(name: "rduser", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "will_retry", type: '${boolean.type}')

            column(name: "workflow_id", type: "BIGINT")

            column(name: "execution_type", type: "VARCHAR(30)")

            column(name: "node_filter_editable", type: '${boolean.type}')
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-7") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "log_file_storage_request")
            }
        }
        createTable(tableName: "log_file_storage_request") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "completed", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "execution_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "filetype", type: "VARCHAR(255)")

            column(name: "last_updated", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "plugin_name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-8") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "node_filter")
            }
        }
        createTable(tableName: "node_filter") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "filter", type: '${text.type}')

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "node_exclude", type: '${text.type}')

            column(name: "node_exclude_name", type: '${text.type}')

            column(name: "node_exclude_os_arch", type: '${text.type}')

            column(name: "node_exclude_os_family", type: '${text.type}')

            column(name: "node_exclude_os_name", type: '${text.type}')

            column(name: "node_exclude_os_version", type: '${text.type}')

            column(name: "node_exclude_precedence", type: '${boolean.type}')

            column(name: "node_exclude_tags", type: '${text.type}')

            column(name: "node_include", type: '${text.type}')

            column(name: "node_include_name", type: '${text.type}')

            column(name: "node_include_os_arch", type: '${text.type}')

            column(name: "node_include_os_family", type: '${text.type}')

            column(name: "node_include_os_name", type: '${text.type}')

            column(name: "node_include_os_version", type: '${text.type}')

            column(name: "node_include_tags", type: '${text.type}')

            column(name: "project", type: "VARCHAR(255)")

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-9") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "notification")
            }
        }
        createTable(tableName: "notification") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "content", type: '${text.type}')

            column(name: "event_trigger", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-10") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "orchestrator")
            }
        }
        createTable(tableName: "orchestrator") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "content", type: '${text.type}')

            column(name: "type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-11") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "plugin_meta")
            }
        }
        createTable(tableName: "plugin_meta") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "json_data", type: "VARCHAR(8192)")

            column(name: "data_key", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-12") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "project")
            }
        }
        createTable(tableName: "project") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "description", type: "VARCHAR(255)")

            column(name: "last_updated", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-13") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "rdoption")
            }
        }
        createTable(tableName: "rdoption") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "default_value", type: '${text.type}')

            column(name: "delimiter", type: "VARCHAR(255)")

            column(name: "description", type: '${text.type}')

            column(name: "enforced", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "multivalued", type: '${boolean.type}')

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "regex", type: '${text.type}')

            column(name: "required", type: '${boolean.type}')

            column(name: "scheduled_execution_id", type: "BIGINT")

            column(name: "secure_exposed", type: '${boolean.type}')

            column(name: "secure_input", type: '${boolean.type}')

            column(name: "sort_index", type: "INT")

            column(name: "values_url", type: "VARCHAR(255)")

            column(name: "values_url_long", type: "VARCHAR(3000)")

            column(name: "default_storage_path", type: "VARCHAR(255)")

            column(name: "date_format", type: "VARCHAR(30)")

            column(name: "is_date", type: '${boolean.type}')
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-14") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "rdoption_values")
            }
        }
        createTable(tableName: "rdoption_values") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "INT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "option_id", type: "BIGINT")

            column(name: "values_string", type: '${text.type}')
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-15") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "rduser")
            }
        }
        createTable(tableName: "rduser") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "dashboard_pref", type: "VARCHAR(255)")

            column(name: "date_created", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "email", type: "VARCHAR(255)")

            column(name: "filter_pref", type: "VARCHAR(255)")

            column(name: "first_name", type: "VARCHAR(255)")

            column(name: "last_name", type: "VARCHAR(255)")

            column(name: "last_updated", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "login", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "password", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-16") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "report_filter")
            }
        }
        createTable(tableName: "report_filter") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "cmd_filter", type: "VARCHAR(255)")

            column(name: "doendafter_filter", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "doendbefore_filter", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "dostartafter_filter", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "dostartbefore_filter", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "endafter_filter", type: "DATETIME")

            column(name: "endbefore_filter", type: "DATETIME")

            column(name: "job_filter", type: "VARCHAR(255)")

            column(name: "job_id_filter", type: "VARCHAR(255)")

            column(name: "mapref_uri_filter", type: "VARCHAR(255)")

            column(name: "message_filter", type: "VARCHAR(255)")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "node_filter", type: "VARCHAR(255)")

            column(name: "obj_filter", type: "VARCHAR(255)")

            column(name: "proj_filter", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "recent_filter", type: "VARCHAR(255)")

            column(name: "report_id_filter", type: "VARCHAR(255)")

            column(name: "startafter_filter", type: "DATETIME")

            column(name: "startbefore_filter", type: "DATETIME")

            column(name: "stat_filter", type: "VARCHAR(255)")

            column(name: "tags_filter", type: "VARCHAR(255)")

            column(name: "title_filter", type: "VARCHAR(255)")

            column(name: "type_filter", type: "VARCHAR(255)")

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "user_filter", type: "VARCHAR(255)")

            column(name: "execnode_filter", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-17") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "scheduled_execution")
            }
        }
        createTable(tableName: "scheduled_execution") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "arg_string", type: '${text.type}')

            column(name: "date_created", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "day_of_month", type: "VARCHAR(255)")

            column(name: "day_of_week", type: "VARCHAR(255)")

            column(name: "description", type: '${text.type}')

            column(name: "do_nodedispatch", type: '${boolean.type}')

            column(name: "exec_count", type: "BIGINT")

            column(name: "filter", type: '${text.type}')

            column(name: "group_path", type: "VARCHAR(2048)")

            column(name: "hour", type: "VARCHAR(255)")

            column(name: "job_name", type: "VARCHAR(1024)") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "loglevel", type: "VARCHAR(255)")

            column(name: "minute", type: "VARCHAR(255)")

            column(name: "month", type: "VARCHAR(255)")

            column(name: "multiple_executions", type: '${boolean.type}')

            column(name: "next_execution", type: "DATETIME")

            column(name: "node_exclude", type: '${text.type}')

            column(name: "node_exclude_name", type: '${text.type}')

            column(name: "node_exclude_os_arch", type: '${text.type}')

            column(name: "node_exclude_os_family", type: '${text.type}')

            column(name: "node_exclude_os_name", type: '${text.type}')

            column(name: "node_exclude_os_version", type: '${text.type}')

            column(name: "node_exclude_precedence", type: '${boolean.type}')

            column(name: "node_exclude_tags", type: '${text.type}')

            column(name: "node_include", type: '${text.type}')

            column(name: "node_include_name", type: '${text.type}')

            column(name: "node_include_os_arch", type: '${text.type}')

            column(name: "node_include_os_family", type: '${text.type}')

            column(name: "node_include_os_name", type: '${text.type}')

            column(name: "node_include_os_version", type: '${text.type}')

            column(name: "node_include_tags", type: '${text.type}')

            column(name: "node_keepgoing", type: '${boolean.type}')

            column(name: "node_rank_attribute", type: "VARCHAR(255)")

            column(name: "node_rank_order_ascending", type: '${boolean.type}')

            column(name: "node_threadcount", type: "INT")

            column(name: "orchestrator_id", type: "BIGINT")

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "retry", type: '${text.type}')

            column(name: "scheduled", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "seconds", type: "VARCHAR(255)")

            column(name: "server_nodeuuid", type: "VARCHAR(36)")

            column(name: "timeout", type: '${text.type}')

            column(name: "total_time", type: "BIGINT")

            column(name: "rduser", type: "VARCHAR(255)")

            column(name: "user_role_list", type: '${text.type}')

            column(name: "uuid", type: "VARCHAR(255)")

            column(name: "workflow_id", type: "BIGINT")

            column(name: "year", type: "VARCHAR(255)")

            column(name: "execution_enabled", type: '${boolean.type}')

            column(name: "log_output_threshold", type: "VARCHAR(256)")

            column(name: "log_output_threshold_action", type: "VARCHAR(256)")

            column(name: "log_output_threshold_status", type: "VARCHAR(256)")

            column(name: "nodes_selected_by_default", type: '${boolean.type}')

            column(name: "schedule_enabled", type: '${boolean.type}')

            column(name: "node_filter_editable", type: '${boolean.type}')
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-18") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "scheduled_execution_filter")
            }
        }
        createTable(tableName: "scheduled_execution_filter") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "desc_filter", type: "VARCHAR(255)")

            column(name: "group_path", type: "VARCHAR(255)")

            column(name: "idlist", type: "VARCHAR(255)")

            column(name: "job_filter", type: "VARCHAR(255)")

            column(name: "loglevel_filter", type: "VARCHAR(255)")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "proj_filter", type: "VARCHAR(255)")

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-19") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "storage")
            }
        }
        createTable(tableName: "storage") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "data", type: "LONGBLOB")

            column(name: "date_created", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "dir", type: "VARCHAR(2048)")

            column(name: "json_data", type: '${text.type}')

            column(name: "last_updated", type: "DATETIME") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(1024)") {
                constraints(nullable: "false")
            }

            column(name: "namespace", type: "VARCHAR(255)")

            column(name: "path_sha", type: "VARCHAR(40)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-20") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "workflow")
            }
        }
        createTable(tableName: "workflow") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "keepgoing", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "strategy", type: "VARCHAR(10)") {
                constraints(nullable: "false")
            }

            column(name: "threadcount", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "plugin_config", type: '${text.type}')
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-21") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "workflow_step")
            }
        }
        createTable(tableName: "workflow_step") {
            column(autoIncrement: '${autoIncrement}', name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "description", type: "VARCHAR(1024)")

            column(name: "error_handler_id", type: "BIGINT")

            column(name: "keepgoing_on_success", type: '${boolean.type}')

            column(name: "class", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "adhoc_execution", type: '${boolean.type}')

            column(name: "adhoc_filepath", type: '${text.type}')

            column(name: "adhoc_local_string", type: '${text.type}')

            column(name: "adhoc_remote_string", type: '${text.type}')

            column(name: "arg_string", type: '${text.type}')

            column(name: "file_extension", type: '${text.type}')

            column(name: "interpreter_args_quoted", type: '${boolean.type}')

            column(name: "script_interpreter", type: '${text.type}')

            column(name: "json_data", type: '${text.type}')

            column(name: "node_step", type: '${boolean.type}')

            column(name: "type", type: "VARCHAR(255)")

            column(name: "job_group", type: "VARCHAR(2048)")

            column(name: "job_name", type: "VARCHAR(1024)")

            column(name: "node_filter", type: '${text.type}')

            column(name: "node_keepgoing", type: '${boolean.type}')

            column(name: "node_rank_attribute", type: '${text.type}')

            column(name: "node_rank_order_ascending", type: '${boolean.type}')

            column(name: "node_threadcount", type: "INT")

            column(name: "node_intersect", type: '${boolean.type}')
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-22") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "workflow_workflow_step")
            }
        }
        createTable(tableName: "workflow_workflow_step") {
            column(name: "workflow_commands_id", type: "BIGINT")

            column(name: "workflow_step_id", type: "BIGINT")

            column(name: "commands_idx", type: "INT")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-41") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(indexName: "UK_pl6hleyex3ofoaklduhbyretw", schemaName: '${default.schema.name}')
            }
        }
        createIndex(indexName: "UK_pl6hleyex3ofoaklduhbyretw", tableName: "auth_token", unique: "true") {
            column(name: "token")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-42") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(indexName: "UK_3k75vvu7mevyvvb5may5lj8k7", schemaName: '${default.schema.name}')
            }
        }
        createIndex(indexName: "UK_3k75vvu7mevyvvb5may5lj8k7", tableName: "project", unique: "true") {
            column(name: "name")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-43") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(indexName: "UK_9dh5owjlbb2x223gopaq6t0jm", schemaName: '${default.schema.name}')
            }
        }
        createIndex(indexName: "UK_9dh5owjlbb2x223gopaq6t0jm", tableName: "report_filter", unique: "true") {
            column(name: "name")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-44") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(indexName: "UK_4t4ehlssm8lvaxieybr42ow5x", schemaName: '${default.schema.name}')
            }
        }
        createIndex(indexName: "UK_4t4ehlssm8lvaxieybr42ow5x", tableName: "scheduled_execution", unique: "true") {
            column(name: "uuid")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-45") {
        preConditions(onFail: "MARK_RAN") {
            not {
                indexExists(indexName: "UK_ow5u86267bvtsol1k9xsox40i", schemaName: '${default.schema.name}')
            }
        }
        createIndex(indexName: "UK_ow5u86267bvtsol1k9xsox40i", tableName: "storage", unique: "true") {
            column(name: "path_sha")
        }
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-23") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_aiqc20kpjasth5bxogsragoif")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "auth_token", constraintName: "FK_aiqc20kpjasth5bxogsragoif", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "rduser", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-24") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_ltiukr4bdti8hclo49o2g6v9o")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "orchestrator_id", baseTableName: "execution", constraintName: "FK_ltiukr4bdti8hclo49o2g6v9o", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "orchestrator", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-25") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_a908hrcn9u20eayg6akkepbl1")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "retry_execution_id", baseTableName: "execution", constraintName: "FK_a908hrcn9u20eayg6akkepbl1", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "execution", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-26") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_6shmc1y7sh51x03aovfualoka")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "execution", constraintName: "FK_6shmc1y7sh51x03aovfualoka", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "scheduled_execution", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-27") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_fofkk7vj9h2bdcqqics5ustr6")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "execution", constraintName: "FK_fofkk7vj9h2bdcqqics5ustr6", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "workflow", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-30") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_trqsa9so0qcv6okcd6fan88yf")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "execution_id", baseTableName: "log_file_storage_request", constraintName: "FK_trqsa9so0qcv6okcd6fan88yf", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "execution", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-31") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_b7y0uh7dsiikf3gxn8rxvp1aj")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "node_filter", constraintName: "FK_b7y0uh7dsiikf3gxn8rxvp1aj", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "rduser", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-32") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_jnk8qdbq6e3wwkcni79wcrewy")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "notification", constraintName: "FK_jnk8qdbq6e3wwkcni79wcrewy", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "scheduled_execution", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-33") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_jraxrdpul0ne9eediy1xjlsfv")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "rdoption", constraintName: "FK_jraxrdpul0ne9eediy1xjlsfv", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "scheduled_execution", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-34") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_p6s9n018km1emn0a0q04ygc72")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "option_id", baseTableName: "rdoption_values", constraintName: "FK_p6s9n018km1emn0a0q04ygc72", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "rdoption", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-35") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_bfos9o64cnd2i2xfnx7il0tix")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "report_filter", constraintName: "FK_bfos9o64cnd2i2xfnx7il0tix", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "rduser", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-36") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_mns7yxw0cx9msuk59x9f2b4lv")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "orchestrator_id", baseTableName: "scheduled_execution", constraintName: "FK_mns7yxw0cx9msuk59x9f2b4lv", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "orchestrator", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-37") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_jln7b7a683uwkl4fg6kpacqnp")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "scheduled_execution", constraintName: "FK_jln7b7a683uwkl4fg6kpacqnp", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "workflow", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-38") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_6r7ok07n10vbsbl6f1ck2h58u")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "scheduled_execution_filter", constraintName: "FK_6r7ok07n10vbsbl6f1ck2h58u", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "rduser", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-39") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_8bbf05v4f6vo5o3cgp69awcue")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "error_handler_id", baseTableName: "workflow_step", constraintName: "FK_8bbf05v4f6vo5o3cgp69awcue", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "workflow_step", referencesUniqueColumn: "false")
    }

    changeSet(author: "rundeck (generated)", id: "1482332299354-40") {
        preConditions(onFail: "MARK_RAN") {
            not {
                foreignKeyConstraintExists(schemaName: '${default.schema.name}', foreignKeyName: "FK_9pkey6k5fdo6worgquakkh7d1")
            }
        }
        addForeignKeyConstraint(baseColumnNames: "workflow_step_id", baseTableName: "workflow_workflow_step", constraintName: "FK_9pkey6k5fdo6worgquakkh7d1", deferrable: "false", initiallyDeferred: "false", onDelete: "NO ACTION", onUpdate: "NO ACTION", referencedColumnNames: "id", referencedTableName: "workflow_step", referencesUniqueColumn: "false")
    }
}
