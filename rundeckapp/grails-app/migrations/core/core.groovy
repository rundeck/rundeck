package core

databaseChangeLog = {

    property name:"bytearray.type",value:"blob", dbms:"mysql"
    property name:"bytearray.type",value:"bytea", dbms:"postgresql"
    property name:"bytearray.type",value:"blob", dbms:"oracle"

    property name:"text.type",value:"longtext", dbms:"mysql"
    property name:"text.type",value:"text", dbms:"postgresql"
    property name:"text.type",value:"longtext", dbms:"oracle"

    changeSet(author: "rundeckuser (generated)", id: "tag-4.3") {
        tagDatabase(tag: "4.3")
    }
    changeSet(author: "rundeckuser (generated)", id: "1598626042931-2") {
        createTable(tableName: "auth_token") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "auth_tokenPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "timestamp")

            column(name: "uuid", type: "VARCHAR(255)")

            column(name: "last_updated", type: "timestamp")

            column(name: "token", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "expiration", type: "timestamp")

            column(name: "auth_roles", type: "CLOB") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "type", type: "VARCHAR(255)")

            column(name: "creator", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-3") {
        createTable(tableName: "base_report") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "base_reportPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "tags", type: "VARCHAR(255)")

            column(name: "ctx_project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "node", type: "VARCHAR(255)")

            column(name: "ctx_type", type: "VARCHAR(255)")

            column(name: "author", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "title", type: "CLOB") {
                constraints(nullable: "false")
            }

            column(name: "message", type: "CLOB") {
                constraints(nullable: "false")
            }

            column(name: "date_completed", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "date_started", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "status", type: "VARCHAR(256)") {
                constraints(nullable: "false")
            }

            column(name: "action_type", type: "VARCHAR(256)") {
                constraints(nullable: "false")
            }

            column(name: "mapref_uri", type: "VARCHAR(255)")

            column(name: "report_id", type: "VARCHAR(3072)")

            column(name: "ctx_name", type: "VARCHAR(255)")

            column(name: "class", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "aborted_by_user", type: "VARCHAR(255)")

            column(name: "ctx_controller", type: "VARCHAR(255)")

            column(name: "succeeded_node_list", type: "CLOB")

            column(name: "ctx_command", type: "VARCHAR(255)")

            column(name: "jc_exec_id", type: "VARCHAR(255)")

            column(name: "jc_job_id", type: "VARCHAR(255)")

            column(name: "failed_node_list", type: "CLOB")

            column(name: "adhoc_execution", type: "BOOLEAN")

            column(name: "adhoc_script", type: "CLOB")

            column(name: "filter_applied", type: "CLOB")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-4") {
        createTable(tableName: "execution") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "executionPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: "BIGINT")

            column(name: "do_nodedispatch", type: "BOOLEAN")

            column(name: "node_exclude_os_arch", type: "CLOB")

            column(name: "node_keepgoing", type: "BOOLEAN")

            column(name: "succeeded_node_list", type: "CLOB")

            column(name: "retry_attempt", type: "INT")

            column(name: "node_include", type: "CLOB")

            column(name: "retry_prev_id", type: "BIGINT")

            column(name: "success_on_empty_node_filter", type: "BOOLEAN")

            column(name: "extra_metadata", type: "CLOB")

            column(name: "node_exclude_os_version", type: "CLOB")

            column(name: "timeout", type: "CLOB")

            column(name: "node_exclude_precedence", type: "BOOLEAN")

            column(name: "node_exclude_name", type: "CLOB")

            column(name: "node_include_os_version", type: "CLOB")

            column(name: "node_exclude_os_name", type: "CLOB")

            column(name: "retry", type: "CLOB")

            column(name: "filter", type: "CLOB")

            column(name: "orchestrator_id", type: "BIGINT")

            column(name: "node_include_name", type: "CLOB")

            column(name: "rduser", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "retry_original_id", type: "BIGINT")

            column(name: "execution_type", type: "VARCHAR(30)")

            column(name: "node_include_os_name", type: "CLOB")

            column(name: "abortedby", type: "VARCHAR(255)")

            column(name: "filter_exclude", type: "CLOB")

            column(name: "node_exclude", type: "CLOB")

            column(name: "node_rank_order_ascending", type: "BOOLEAN")

            column(name: "node_include_os_arch", type: "CLOB")

            column(name: "loglevel", type: "VARCHAR(255)")

            column(name: "node_exclude_os_family", type: "CLOB")

            column(name: "node_include_os_family", type: "CLOB")

            column(name: "cancelled", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "retry_delay", type: "CLOB")

            column(name: "workflow_id", type: "BIGINT")

            column(name: "timed_out", type: "BOOLEAN")

            column(name: "failed_node_list", type: "CLOB")

            column(name: "arg_string", type: "CLOB")

            column(name: "user_role_list", type: "CLOB")

            column(name: "node_rank_attribute", type: "VARCHAR(255)")

            column(name: "date_completed", type: "timestamp")

            column(name: "outputfilepath", type: "CLOB")

            column(name: "server_nodeuuid", type: "VARCHAR(36)")

            column(name: "will_retry", type: "BOOLEAN")

            column(name: "retry_execution_id", type: "BIGINT")

            column(name: "node_exclude_tags", type: "CLOB")

            column(name: "exclude_filter_uncheck", type: "BOOLEAN")

            column(name: "node_threadcount", type: "INT")

            column(name: "node_include_tags", type: "CLOB")

            column(name: "date_started", type: "timestamp")

            column(name: "status", type: "VARCHAR(255)")

            column(name: "node_filter_editable", type: "BOOLEAN")

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-5") {
        createTable(tableName: "job_file_record") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "job_file_recordPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "uuid", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "SIZE", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "record_name", type: "VARCHAR(255)")

            column(name: "storage_reference", type: "CLOB") {
                constraints(nullable: "false")
            }

            column(name: "file_state", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "file_name", type: "VARCHAR(1024)")

            column(name: "storage_type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "storage_meta", type: "CLOB")

            column(name: "record_type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "execution_id", type: "BIGINT")

            column(name: "server_nodeuuid", type: "VARCHAR(255)")

            column(name: "rduser", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "job_id", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "sha", type: "VARCHAR(64)") {
                constraints(nullable: "false")
            }

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "expiration_date", type: "timestamp")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-6") {
        createTable(tableName: "log_file_storage_request") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "log_file_storage_requestPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "plugin_name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "filetype", type: "VARCHAR(255)")

            column(name: "last_updated", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "completed", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "execution_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-7") {
        createTable(tableName: "node_filter") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "node_filterPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "node_exclude_os_family", type: "CLOB")

            column(name: "node_include_os_family", type: "CLOB")

            column(name: "node_exclude_os_arch", type: "CLOB")

            column(name: "node_include", type: "CLOB")

            column(name: "node_exclude_os_version", type: "CLOB")

            column(name: "node_exclude_precedence", type: "BOOLEAN")

            column(name: "node_exclude_name", type: "CLOB")

            column(name: "node_include_os_version", type: "CLOB")

            column(name: "node_exclude_os_name", type: "CLOB")

            column(name: "filter", type: "CLOB")

            column(name: "node_exclude_tags", type: "CLOB")

            column(name: "node_include_tags", type: "CLOB")

            column(name: "node_include_name", type: "CLOB")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "node_include_os_name", type: "CLOB")

            column(name: "node_exclude", type: "CLOB")

            column(name: "node_include_os_arch", type: "CLOB")

            column(name: "project", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-8") {
        createTable(tableName: "notification") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "notificationPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "event_trigger", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "content", type: "CLOB")

            column(name: "format", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-9") {
        createTable(tableName: "orchestrator") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "orchestratorPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "type", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "content", type: "CLOB")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-10") {
        createTable(tableName: "plugin_meta") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "plugin_metaPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "json_data", type: "CLOB")

            column(name: "date_created", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "data_key", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-11") {
        createTable(tableName: "project") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "projectPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "last_updated", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "description", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-12") {
        createTable(tableName: "rdoption") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "rdoptionPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: "BIGINT")

            column(name: "default_storage_path", type: "VARCHAR(255)")

            column(name: "sort_values", type: "BOOLEAN")

            column(name: "option_type", type: "CLOB")

            column(name: "values_url", type: "VARCHAR(255)")

            column(name: "enforced", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "values_url_long", type: "VARCHAR(3000)")

            column(name: "multivalued", type: "BOOLEAN")

            column(name: "delimiter", type: "VARCHAR(255)")

            column(name: "values_list_delimiter", type: "VARCHAR(255)")

            column(name: "secure_exposed", type: "BOOLEAN")

            column(name: "option_values_plugin_type", type: "VARCHAR(255)")

            column(name: "is_date", type: "BOOLEAN")

            column(name: "default_value", type: "CLOB")

            column(name: "config_data", type: "CLOB")

            column(name: "sort_index", type: "INT")

            column(name: "secure_input", type: "BOOLEAN")

            column(name: "hidden", type: "BOOLEAN")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "regex", type: "CLOB")

            column(name: "required", type: "BOOLEAN")

            column(name: "date_format", type: "VARCHAR(30)")

            column(name: "multivalue_all_selected", type: "BOOLEAN")

            column(name: "values_list", type: "CLOB")

            column(name: "label", type: "VARCHAR(255)")

            column(name: "description", type: "CLOB")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-13") {
        createTable(tableName: "rdoption_values") {
            column(name: "option_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "values_string", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-14") {
        createTable(tableName: "rduser") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "rduserPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "last_logged_host_name", type: "VARCHAR(255)")

            column(name: "login", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "first_name", type: "VARCHAR(255)")

            column(name: "last_updated", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "last_logout", type: "timestamp")

            column(name: "filter_pref", type: "VARCHAR(255)")

            column(name: "dashboard_pref", type: "VARCHAR(255)")

            column(name: "password", type: "VARCHAR(255)")

            column(name: "last_session_id", type: "VARCHAR(255)")

            column(name: "last_name", type: "VARCHAR(255)")

            column(name: "email", type: "VARCHAR(255)")

            column(name: "last_login", type: "timestamp")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-15") {
        createTable(tableName: "referenced_execution") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "referenced_executionPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: "BIGINT")

            column(name: "status", type: "VARCHAR(255)")

            column(name: "execution_id", type: "BIGINT") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-16") {
        createTable(tableName: "report_filter") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "report_filterPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "message_filter", type: "VARCHAR(255)")

            column(name: "title_filter", type: "VARCHAR(255)")

            column(name: "startafter_filter", type: "timestamp")

            column(name: "startbefore_filter", type: "timestamp")

            column(name: "job_id_filter", type: "VARCHAR(255)")

            column(name: "type_filter", type: "VARCHAR(255)")

            column(name: "execnode_filter", type: "VARCHAR(255)")

            column(name: "recent_filter", type: "VARCHAR(255)")

            column(name: "dostartafter_filter", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "doendbefore_filter", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "doendafter_filter", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "report_id_filter", type: "VARCHAR(255)")

            column(name: "node_filter", type: "VARCHAR(255)")

            column(name: "obj_filter", type: "VARCHAR(255)")

            column(name: "stat_filter", type: "VARCHAR(255)")

            column(name: "cmd_filter", type: "VARCHAR(255)")

            column(name: "dostartbefore_filter", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "tags_filter", type: "VARCHAR(255)")

            column(name: "user_filter", type: "VARCHAR(255)")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "mapref_uri_filter", type: "VARCHAR(255)")

            column(name: "proj_filter", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "endbefore_filter", type: "timestamp")

            column(name: "endafter_filter", type: "timestamp")

            column(name: "job_filter", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-17") {
        createTable(tableName: "scheduled_execution") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "scheduled_executionPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "log_output_threshold", type: "VARCHAR(256)")

            column(name: "do_nodedispatch", type: "BOOLEAN")

            column(name: "next_execution", type: "timestamp")

            column(name: "date_created", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "node_keepgoing", type: "BOOLEAN")

            column(name: "node_exclude_os_arch", type: "CLOB")

            column(name: "uuid", type: "VARCHAR(255)")

            column(name: "node_include", type: "CLOB")

            column(name: "success_on_empty_node_filter", type: "BOOLEAN")

            column(name: "node_exclude_os_version", type: "CLOB")

            column(name: "timeout", type: "CLOB")

            column(name: "node_exclude_precedence", type: "BOOLEAN")

            column(name: "node_exclude_name", type: "CLOB")

            column(name: "notify_avg_duration_threshold", type: "CLOB")

            column(name: "day_of_week", type: "VARCHAR(255)")

            column(name: "node_include_os_version", type: "CLOB")

            column(name: "node_exclude_os_name", type: "CLOB")

            column(name: "retry", type: "CLOB")

            column(name: "filter", type: "CLOB")

            column(name: "group_path", type: "VARCHAR(2048)")

            column(name: "scheduled", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "orchestrator_id", type: "BIGINT")

            column(name: "node_threadcount_dynamic", type: "VARCHAR(255)")

            column(name: "node_include_name", type: "CLOB")

            column(name: "multiple_executions", type: "BOOLEAN")

            column(name: "time_zone", type: "VARCHAR(256)")

            column(name: "rduser", type: "VARCHAR(255)")

            column(name: "node_include_os_name", type: "CLOB")

            column(name: "filter_exclude", type: "CLOB")

            column(name: "node_exclude", type: "CLOB")

            column(name: "node_rank_order_ascending", type: "BOOLEAN")

            column(name: "nodes_selected_by_default", type: "BOOLEAN")

            column(name: "node_include_os_arch", type: "CLOB")

            column(name: "loglevel", type: "VARCHAR(255)")

            column(name: "node_exclude_os_family", type: "CLOB")

            column(name: "execution_enabled", type: "BOOLEAN")

            column(name: "max_multiple_executions", type: "VARCHAR(256)")

            column(name: "node_include_os_family", type: "CLOB")

            column(name: "last_updated", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "retry_delay", type: "CLOB")

            column(name: "workflow_id", type: "BIGINT")

            column(name: "exec_count", type: "BIGINT")

            column(name: "month", type: "VARCHAR(255)")

            column(name: "hour", type: "VARCHAR(255)")

            column(name: "log_output_threshold_action", type: "VARCHAR(256)")

            column(name: "arg_string", type: "CLOB")

            column(name: "user_role_list", type: "CLOB")

            column(name: "total_time", type: "BIGINT")

            column(name: "node_rank_attribute", type: "VARCHAR(255)")

            column(name: "server_nodeuuid", type: "VARCHAR(36)")

            column(name: "default_tab", type: "VARCHAR(256)")

            column(name: "node_exclude_tags", type: "CLOB")

            column(name: "seconds", type: "VARCHAR(255)")

            column(name: "exclude_filter_uncheck", type: "BOOLEAN")

            column(name: "ref_exec_count", type: "BIGINT")

            column(name: "node_threadcount", type: "INT")

            column(name: "node_include_tags", type: "CLOB")

            column(name: "job_name", type: "VARCHAR(1024)") {
                constraints(nullable: "false")
            }

            column(name: "schedule_enabled", type: "BOOLEAN")

            column(name: "year", type: "VARCHAR(255)")

            column(name: "day_of_month", type: "VARCHAR(255)")

            column(name: "node_filter_editable", type: "BOOLEAN")

            column(name: "log_output_threshold_status", type: "VARCHAR(256)")

            column(name: "description", type: "CLOB")

            column(name: "minute", type: "VARCHAR(255)")

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "plugin_config", type: "CLOB")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-18") {
        createTable(tableName: "scheduled_execution_filter") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "scheduled_execution_filterPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "loglevel_filter", type: "VARCHAR(255)")

            column(name: "group_path", type: "VARCHAR(255)")

            column(name: "server_nodeuuidfilter", type: "VARCHAR(255)")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "idlist", type: "VARCHAR(255)")

            column(name: "user_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "scheduled_filter", type: "VARCHAR(255)")

            column(name: "proj_filter", type: "VARCHAR(255)")

            column(name: "desc_filter", type: "VARCHAR(255)")

            column(name: "job_filter", type: "VARCHAR(255)")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-19") {
        createTable(tableName: "scheduled_execution_stats") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "scheduled_execution_statsPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "se_id", type: "BIGINT")

            column(name: "content", type: "CLOB") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-20") {
        createTable(tableName: "storage") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "storagePK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "json_data", type: "CLOB")

            column(name: "date_created", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "dir", type: "VARCHAR(2048)")

            column(name: "last_updated", type: "timestamp") {
                constraints(nullable: "false")
            }

            column(name: "name", type: "VARCHAR(1024)") {
                constraints(nullable: "false")
            }

            column(name: "namespace", type: "VARCHAR(255)")

            column(name: "data", type: '${bytearray.type}')

            column(name: "path_sha", type: "VARCHAR(40)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-21") {
        createTable(tableName: "webhook") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "webhookPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "event_plugin", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "plugin_configuration_json", type: "CLOB") {
                constraints(nullable: "false")
            }

            column(name: "uuid", type: "VARCHAR(255)")

            column(name: "name", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "enabled", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "project", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "auth_token", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-22") {
        createTable(tableName: "workflow") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "workflowPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "strategy", type: "VARCHAR(256)") {
                constraints(nullable: "false")
            }

            column(name: "keepgoing", type: "BOOLEAN") {
                constraints(nullable: "false")
            }

            column(name: "threadcount", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "plugin_config", type: "CLOB")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-23") {
        createTable(tableName: "workflow_step") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "workflow_stepPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "plugin_config_data", type: "CLOB")

            column(name: "keepgoing_on_success", type: "BOOLEAN")

            column(name: "error_handler_id", type: "BIGINT")

            column(name: "description", type: "VARCHAR(1024)")

            column(name: "class", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "file_extension", type: "CLOB")

            column(name: "interpreter_args_quoted", type: "BOOLEAN")

            column(name: "adhoc_execution", type: "BOOLEAN")

            column(name: "arg_string", type: "CLOB")

            column(name: "adhoc_filepath", type: "CLOB")

            column(name: "adhoc_remote_string", type: "CLOB")

            column(name: "script_interpreter", type: "CLOB")

            column(name: "adhoc_local_string", type: "CLOB")

            column(name: "json_data", type: "CLOB")

            column(name: "node_step", type: "BOOLEAN")

            column(name: "type", type: "VARCHAR(255)")

            column(name: "fail_on_disable", type: "BOOLEAN")

            column(name: "node_keepgoing", type: "BOOLEAN")

            column(name: "uuid", type: "CLOB")

            column(name: "ignore_notifications", type: "BOOLEAN")

            column(name: "node_intersect", type: "BOOLEAN")

            column(name: "node_filter", type: "CLOB")

            column(name: "node_rank_attribute", type: "CLOB")

            column(name: "node_threadcount", type: "INT")

            column(name: "job_project", type: "VARCHAR(2048)")

            column(name: "job_name", type: "VARCHAR(1024)")

            column(name: "use_name", type: "BOOLEAN")

            column(name: "job_group", type: "VARCHAR(2048)")

            column(name: "import_options", type: "BOOLEAN")

            column(name: "node_rank_order_ascending", type: "BOOLEAN")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-24") {
        createTable(tableName: "workflow_workflow_step") {
            column(name: "workflow_commands_id", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "workflow_step_id", type: "BIGINT")

            column(name: "commands_idx", type: "INT")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-25") {
        addUniqueConstraint(columnNames: "token", constraintName: "UC_AUTH_TOKENTOKEN_COL", tableName: "auth_token")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-26") {
        addUniqueConstraint(columnNames: "execution_id", constraintName: "UC_LOG_FILE_STORAGE_REQUESTEXECUTION_ID_COL", tableName: "log_file_storage_request")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-27") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_PROJECTNAME_COL", tableName: "project")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-28") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_REPORT_FILTERNAME_COL", tableName: "report_filter")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-29") {
        addUniqueConstraint(columnNames: "uuid", constraintName: "UC_SCHEDULED_EXECUTIONUUID_COL", tableName: "scheduled_execution")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-30") {
        addUniqueConstraint(columnNames: "path_sha", constraintName: "UC_STORAGEPATH_SHA_COL", tableName: "storage")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-31") {
        createIndex(indexName: "BASE_REPORT_IDX_2", tableName: "base_report") {
            column(name: "ctx_project")

            column(name: "date_completed")

            column(name: "date_started")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-32") {
        createIndex(indexName: "EXEC_IDX_1", tableName: "execution") {
            column(defaultValueComputed: "nextval('execution_id_seq'::regclass)", name: "id")

            column(name: "date_completed")

            column(name: "project")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-33") {
        createIndex(indexName: "EXEC_IDX_2", tableName: "execution") {
            column(name: "date_started")

            column(name: "status")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-34") {
        createIndex(indexName: "EXEC_IDX_3", tableName: "execution") {
            column(name: "date_completed")

            column(name: "project")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-35") {
        createIndex(indexName: "EXEC_IDX_4", tableName: "execution") {
            column(name: "scheduled_execution_id")

            column(name: "date_completed")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-36") {
        createIndex(indexName: "EXEC_IDX_5", tableName: "execution") {
            column(name: "scheduled_execution_id")

            column(name: "status")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-37") {
        createIndex(indexName: "EXEC_IDX_6", tableName: "execution") {
            column(name: "rduser")

            column(name: "date_started")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-38") {
        createIndex(indexName: "EXEC_REPORT_IDX_0", tableName: "base_report") {
            column(name: "ctx_project")

            column(name: "date_completed")

            column(name: "jc_exec_id")

            column(name: "jc_job_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-39") {
        createIndex(indexName: "EXEC_REPORT_IDX_1", tableName: "base_report") {
            column(name: "ctx_project")

            column(name: "jc_job_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-40") {
        createIndex(indexName: "EXEC_REPORT_IDX_2", tableName: "base_report") {
            column(name: "jc_exec_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-41") {
        createIndex(indexName: "IDX_TYPE", tableName: "auth_token") {
            column(name: "type")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-42") {
        createIndex(indexName: "JOB_IDX_PROJECT", tableName: "scheduled_execution") {
            column(name: "project")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-43") {
        createIndex(indexName: "REFEXEC_IDX_1", tableName: "referenced_execution") {
            column(name: "scheduled_execution_id")

            column(name: "status")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-44") {
        createIndex(indexName: "STORAGE_IDX_NAMESPACE", tableName: "storage") {
            column(name: "namespace")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-45") {
        createIndex(indexName: "WORKFLOW_COMMANDS_IDX_0", tableName: "workflow_workflow_step") {
            column(name: "workflow_commands_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-46") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "scheduled_execution_filter", constraintName: "FK22545y15qs4iqod1ljyqsm1fi", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-47") {
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "scheduled_execution", constraintName: "FK3d7mwn5cy49tddfba3ewvftgk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-48") {
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "notification", constraintName: "FK3n044fm81867b5putknoupx36", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-49") {
        addForeignKeyConstraint(baseColumnNames: "execution_id", baseTableName: "referenced_execution", constraintName: "FK3sv28w2o5i03gxi66b80240qk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-50") {
        addForeignKeyConstraint(baseColumnNames: "execution_id", baseTableName: "log_file_storage_request", constraintName: "FK45h35d8rum4i3ruqomwgs0n49", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-51") {
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "referenced_execution", constraintName: "FK5t1rmnkvaaj8haakvh0al8uhq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-52") {
        addForeignKeyConstraint(baseColumnNames: "retry_execution_id", baseTableName: "execution", constraintName: "FK77u7nheh1hwykxnnh6y17ldvc", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-53") {
        addForeignKeyConstraint(baseColumnNames: "orchestrator_id", baseTableName: "scheduled_execution", constraintName: "FK7b7mj9danbl00nokdrq1iyn23", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "orchestrator", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-54") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "auth_token", constraintName: "FK8kjy551iwoqqmsu2f1a88pwbu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-55") {
        addForeignKeyConstraint(baseColumnNames: "execution_id", baseTableName: "job_file_record", constraintName: "FK9u6t22sfe6v8hfj3kj2sjg9rg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-56") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "report_filter", constraintName: "FKcdly7hl8164nfb0e5908ch64n", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-57") {
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "execution", constraintName: "FKdtqy2uar1ln372ne8u2jj27nw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-58") {
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "rdoption", constraintName: "FKe75poq1n8bmqu5ireg3fjc7iu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-59") {
        addForeignKeyConstraint(baseColumnNames: "orchestrator_id", baseTableName: "execution", constraintName: "FKgm5hu46fmti2jpbj1d7xlyw65", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "orchestrator", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-60") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "node_filter", constraintName: "FKhuj3n3gy2kqoie8d8oucay4b3", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-61") {
        addForeignKeyConstraint(baseColumnNames: "error_handler_id", baseTableName: "workflow_step", constraintName: "FKipk3uik8eqp54cf0ul996bu4o", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_step", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-62") {
        addForeignKeyConstraint(baseColumnNames: "option_id", baseTableName: "rdoption_values", constraintName: "FKnm8rfqbotpdp347xqsltkegfv", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rdoption", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-63") {
        addForeignKeyConstraint(baseColumnNames: "se_id", baseTableName: "scheduled_execution_stats", constraintName: "FKpwc0snw00ehserhlpgftbu8aw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-64") {
        addForeignKeyConstraint(baseColumnNames: "workflow_step_id", baseTableName: "workflow_workflow_step", constraintName: "FKrln65du8n7hwu3qbinnea748g", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_step", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "1598626042931-65") {
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "execution", constraintName: "FKs7kalwep0lr5r39cntcu0pev6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow", validate: "true")
    }
}
