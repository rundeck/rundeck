import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import liquibase.statement.core.UpdateStatement

databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-4") {
        preConditions(onFail: "MARK_RAN"){
            not{
                tableExists (tableName:"execution")
            }
        }

        createTable(tableName: "execution") {
            column(autoIncrement: "true", name: "id", type: '${number.type}') {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "executionPK")
            }

            column(name: "version", type: '${number.type}') {
                constraints(nullable: "false")
            }

            column(name: "scheduled_execution_id", type: '${number.type}')

            column(name: "do_nodedispatch", type: '${boolean.type}')

            column(name: "node_exclude_os_arch", type: '${text.type}')

            column(name: "node_keepgoing", type: '${boolean.type}')

            column(name: "succeeded_node_list", type: '${text.type}')

            column(name: "retry_attempt", type: '${int.type}')

            column(name: "node_include", type: '${text.type}')

            column(name: "retry_prev_id", type: '${number.type}')

            column(name: "success_on_empty_node_filter", type: '${boolean.type}')

            column(name: "extra_metadata", type: '${text.type}')

            column(name: "node_exclude_os_version", type: '${text.type}')

            column(name: "timeout", type: '${text.type}')

            column(name: "node_exclude_precedence", type: '${boolean.type}')

            column(name: "node_exclude_name", type: '${text.type}')

            column(name: "node_include_os_version", type: '${text.type}')

            column(name: "node_exclude_os_name", type: '${text.type}')

            column(name: "retry", type: '${text.type}')

            column(name: "filter", type: '${text.type}')

            column(name: "orchestrator_id", type: '${number.type}')

            column(name: "node_include_name", type: '${text.type}')

            column(name: "rduser", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }

            column(name: "retry_original_id", type: '${number.type}')

            column(name: "execution_type", type: '${varchar30.type}')

            column(name: "node_include_os_name", type: '${text.type}')

            column(name: "abortedby", type: '${varchar255.type}')

            column(name: "filter_exclude", type: '${text.type}')

            column(name: "node_exclude", type: '${text.type}')

            column(name: "node_rank_order_ascending", type: '${boolean.type}')

            column(name: "node_include_os_arch", type: '${text.type}')

            column(name: "loglevel", type: '${varchar255.type}')

            column(name: "node_exclude_os_family", type: '${text.type}')

            column(name: "node_include_os_family", type: '${text.type}')

            column(name: "cancelled", type: '${boolean.type}') {
                constraints(nullable: "false")
            }

            column(name: "retry_delay", type: '${text.type}')

            column(name: "workflow_id", type: '${number.type}')

            column(name: "timed_out", type: '${boolean.type}')

            column(name: "failed_node_list", type: '${text.type}')

            column(name: "arg_string", type: '${text.type}')

            column(name: "user_role_list", type: '${text.type}')

            column(name: "node_rank_attribute", type: '${varchar255.type}')

            column(name: "date_completed", type: '${timestamp.type}')

            column(name: "outputfilepath", type: '${text.type}')

            column(name: "server_nodeuuid", type: '${varchar36.type}')

            column(name: "will_retry", type: '${boolean.type}')

            column(name: "retry_execution_id", type: '${number.type}')

            column(name: "node_exclude_tags", type: '${text.type}')

            column(name: "exclude_filter_uncheck", type: '${boolean.type}')

            column(name: "node_threadcount", type: '${int.type}')

            column(name: "node_include_tags", type: '${text.type}')

            column(name: "date_started", type: '${timestamp.type}')

            column(name: "status", type: '${varchar255.type}')

            column(name: "node_filter_editable", type: '${boolean.type}')

            column(name: "project", type: '${varchar255.type}') {
                constraints(nullable: "false")
            }
        }
    }
    changeSet(author: "rundeckuser (generated)", failOnError:"false", id: "3.4.0-5", dbms: "h2") {
        comment { 'rename "filter" to FILTER' }
        preConditions(onFail: 'MARK_RAN') {
            grailsPrecondition {
                check {
                    def ran = sql.firstRow("SELECT count(*) as num FROM INFORMATION_SCHEMA.columns where table_name ='EXECUTION' and column_name  = 'filter'").num
                    if(ran==0) fail('precondition is not satisfied')
                }
            }
        }
        grailsChange {
            change {
                sql.execute("ALTER TABLE execution RENAME COLUMN \"filter\" TO FILTER;")
            }
            rollback {
            }
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "4.14.0-add-workflow-id-index") {
        preConditions(onFail: "MARK_RAN"){
            not {
                indexExists(indexName: "execution_workflow_id_idx", tableName: "execution")
            }
        }

        createIndex(indexName: "execution_workflow_id_idx", tableName: "execution", unique: false) {
            column(name: "workflow_id")
        }
    }

    changeSet(author: "rundeckdev", id: "4.x-add-uuid-and-job-uuid") {
        preConditions(onFail: "MARK_RAN") {
            not {
                columnExists(tableName: "execution", columnName: 'uuid')
                columnExists(tableName: "execution", columnName: 'job_uuid')
            }
        }
        addColumn(tableName: "execution") {
            column(name: 'job_uuid', type: '${varchar255.type}')
            column(name: 'uuid', type: '${varchar255.type}')
        }

    }

    changeSet(author: "rundeckdev", id: "4.x-add-uuid-and-job-uuid-indexes") {
        preConditions(onFail: "MARK_RAN"){
            not {
                indexExists(indexName: "execution_uuid_idx", tableName: "execution")
                indexExists(indexName: "execution_job_uuid_idx", tableName: "execution")
            }
        }
        createIndex(indexName: "execution_uuid_idx", tableName: "execution") {
            column(name: "uuid")
        }
        createIndex(indexName: "execution_job_uuid_idx", tableName: "execution") {
            column(name: "job_uuid")
        }
    }

    changeSet(author: "rundeckdev", id: "4.x-populate-job-uuid") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "execution")
            tableExists(tableName: "scheduled_execution")
        }
        sql("update execution set job_uuid = (select scheduled_execution.uuid from scheduled_execution where scheduled_execution.id = execution.scheduled_execution_id) where job_uuid is null")
    }
    changeSet(author: "rundeckuser (generated)", failOnError:"true", id: "all-executions-have-uuids") {
        preConditions(onFail: 'MARK_RAN') {
            columnExists(tableName: "execution", columnName: 'uuid')
        }
        grailsChange {
            change {
                def updates = []
                sql.eachRow("select id from execution") {

                    updates.add(new UpdateStatement(null,null,"execution")
                            .addNewColumnValue("uuid",UUID.randomUUID().toString())
                            .setWhereClause("id = '${it.id}'"))
                }

                sqlStatements(updates)
            }
            rollback {
            }
        }
    }
}