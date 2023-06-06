databaseChangeLog = {
    changeSet(author: "rundeckuser (generated)", id: "3.4.0-26") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"workflow")
            }
        }
        createTable(tableName: "workflow") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "workflowPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "strategy", type: '${varchar256.type}') {
                constraints(nullable: "false")
            }

            column(name: "keepgoing", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "threadcount", type: '${int.type}') {
                constraints(nullable: "false")
            }

            column(name: "plugin_config", type: '${text.type}')
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-27") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"workflow_step")
            }
        }
        createTable(tableName: "workflow_step") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "workflow_stepPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "plugin_config_data", type: '${text.type}')

            column(name: "keepgoing_on_success", type: '${boolean.type}')

            column(name: "error_handler_id", type: '${number.type}')

            column(name: "description", type: '${varchar1024.type}')

            column(name: "class", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "json_data", type: '${text.type}')

            column(name: "node_step", type: '${boolean.type}')

            column(name: "type", type: '${varchar255.type}')

            column(name: "fail_on_disable", type: '${boolean.type}')

            column(name: "node_keepgoing", type: '${boolean.type}')

            column(name: "uuid", type: '${text.type}')

            column(name: "ignore_notifications", type: '${boolean.type}')

            column(name: "node_intersect", type: '${boolean.type}')

            column(name: "node_filter", type: '${text.type}')

            column(name: "arg_string", type: '${text.type}')

            column(name: "child_nodes", type: '${boolean.type}')

            column(name: "node_rank_attribute", type: '${text.type}')

            column(name: "node_threadcount", type: '${int.type}')

            column(name: "job_project", type: '${varchar2048.type}')

            column(name: "job_name", type: '${varchar1024.type}')

            column(name: "use_name", type: '${boolean.type}')

            column(name: "job_group", type: '${varchar2048.type}')

            column(name: "import_options", type: '${boolean.type}')

            column(name: "node_rank_order_ascending", type: '${boolean.type}')

            column(name: "file_extension", type: '${text.type}')

            column(name: "interpreter_args_quoted", type: '${boolean.type}')

            column(name: "adhoc_execution", type: '${boolean.type}')

            column(name: "expand_token_in_script_file", type: '${boolean.type}')

            column(name: "adhoc_filepath", type: '${text.type}')

            column(name: "adhoc_remote_string", type: '${text.type}')

            column(name: "script_interpreter", type: '${text.type}')

            column(name: "adhoc_local_string", type: '${text.type}')
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-28") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"workflow_workflow_step")
            }
        }
        createTable(tableName: "workflow_workflow_step") {
            column(name: "workflow_commands_id", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "workflow_step_id", type: '${number.type}')

            column(name: "commands_idx", type: '${int.type}')
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "4.14.0-add-index-workflow-workflow-step-id"){
        preConditions(onFail: "MARK_RAN"){
            not{
                indexExists(indexName: "workflow_workflow_step_workflow_step_id_idx", tableName: "workflow_workflow_step")
            }
        }

        createIndex(indexName: "workflow_workflow_step_workflow_step_id_idx", tableName: "workflow_workflow_step", unique: false) {
            column(name: "workflow_step_id")
        }
    }
}