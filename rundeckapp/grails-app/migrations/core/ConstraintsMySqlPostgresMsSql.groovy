databaseChangeLog = {

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-29", dbms:"mysql,mssql,postgresql") {
        addUniqueConstraint(columnNames: "token", constraintName: "UC_AUTH_TOKENTOKEN_COL", tableName: "auth_token")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-30", dbms:"mysql,mssql,postgresql") {
        addUniqueConstraint(columnNames: "execution_id", constraintName: "UC_LOG_FILE_STORAGE_REQUESTEXECUTION_ID_COL", tableName: "log_file_storage_request")
    }


    changeSet(author: "rundeckuser (generated)", id: "3.4.0-31", dbms:"mysql,mssql,postgresql") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_PROJECTNAME_COL", tableName: "project")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-32", dbms:"mysql,mssql,postgresql") {
        addUniqueConstraint(columnNames: "name", constraintName: "UC_REPORT_FILTERNAME_COL", tableName: "report_filter")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-33", dbms:"mysql,mssql,postgresql") {
        addUniqueConstraint(columnNames: "uuid", constraintName: "UC_SCHEDULED_EXECUTIONUUID_COL", tableName: "scheduled_execution")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-34", dbms:"mysql,mssql,postgresql") {
        addUniqueConstraint(columnNames: "path_sha", constraintName: "UC_STORAGEPATH_SHA_COL", tableName: "storage")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-35", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "BASE_REPORT_IDX_2", tableName: "base_report") {
            column(name: "ctx_project")

            column(name: "date_completed")

            column(name: "date_started")
        }
    }


    changeSet(author: "rundeckuser (generated)", id: "3.4.0-36", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_IDX_1", tableName: "execution") {
            column(defaultValueComputed: "nextval('execution_id_seq'::regclass)", name: "id")

            column(name: "date_completed")

            column(name: "project")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-37", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_IDX_2", tableName: "execution") {
            column(name: "date_started")

            column(name: "status")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-38", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_IDX_3", tableName: "execution") {
            column(name: "date_completed")

            column(name: "project")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-39", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_IDX_4", tableName: "execution") {
            column(name: "scheduled_execution_id")

            column(name: "date_completed")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-40", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_IDX_5", tableName: "execution") {
            column(name: "scheduled_execution_id")

            column(name: "status")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-41", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_IDX_6", tableName: "execution") {
            column(name: "rduser")

            column(name: "date_started")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-42", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_REPORT_IDX_0", tableName: "base_report") {
            column(name: "ctx_project")

            column(name: "date_completed")

            column(name: "jc_exec_id")

            column(name: "jc_job_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-43", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_REPORT_IDX_1", tableName: "base_report") {
            column(name: "ctx_project")

            column(name: "jc_job_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-44", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "EXEC_REPORT_IDX_2", tableName: "base_report") {
            column(name: "jc_exec_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-45", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "IDX_TYPE", tableName: "auth_token") {
            column(name: "type")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-46", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "JOB_IDX_PROJECT", tableName: "scheduled_execution") {
            column(name: "project")
        }
    }


    changeSet(author: "rundeckuser (generated)", id: "3.4.0-47", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "REFEXEC_IDX_1", tableName: "referenced_execution") {
            column(name: "scheduled_execution_id")

            column(name: "status")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-48", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "STORAGE_IDX_NAMESPACE", tableName: "storage") {
            column(name: "namespace")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-49", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "STORED_EVENT_IDX_LAST_UPDATED", tableName: "stored_event") {
            column(name: "last_updated")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-50", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "STORED_EVENT_IDX_OBJECT_ID", tableName: "stored_event") {
            column(name: "object_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-51", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "STORED_EVENT_IDX_PROJECT_NAME", tableName: "stored_event") {
            column(name: "project_name")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-52", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "STORED_EVENT_IDX_SEQUENCE", tableName: "stored_event") {
            column(name: "sequence")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-53", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "STORED_EVENT_IDX_SUBSYSTEM", tableName: "stored_event") {
            column(name: "subsystem")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-54", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "STORED_EVENT_IDX_TOPIC", tableName: "stored_event") {
            column(name: "topic")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-55", dbms:"mysql,mssql,postgresql") {
        createIndex(indexName: "WORKFLOW_COMMANDS_IDX_0", tableName: "workflow_workflow_step") {
            column(name: "workflow_commands_id")
        }
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-56", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "scheduled_execution_filter", constraintName: "FK22545y15qs4iqod1ljyqsm1fi", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-57", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "scheduled_execution", constraintName: "FK3d7mwn5cy49tddfba3ewvftgk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-58", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "notification", constraintName: "FK3n044fm81867b5putknoupx36", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-59", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "execution_id", baseTableName: "referenced_execution", constraintName: "FK3sv28w2o5i03gxi66b80240qk", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-60", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "execution_id", baseTableName: "log_file_storage_request", constraintName: "FK45h35d8rum4i3ruqomwgs0n49", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-61", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "referenced_execution", constraintName: "FK5t1rmnkvaaj8haakvh0al8uhq", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-62", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "retry_execution_id", baseTableName: "execution", constraintName: "FK77u7nheh1hwykxnnh6y17ldvc", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-63", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "orchestrator_id", baseTableName: "scheduled_execution", constraintName: "FK7b7mj9danbl00nokdrq1iyn23", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "orchestrator", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-64", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "auth_token", constraintName: "FK8kjy551iwoqqmsu2f1a88pwbu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-65", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "execution_id", baseTableName: "job_file_record", constraintName: "FK9u6t22sfe6v8hfj3kj2sjg9rg", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-66", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "report_filter", constraintName: "FKcdly7hl8164nfb0e5908ch64n", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }


    changeSet(author: "rundeckuser (generated)", id: "3.4.0-67", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "execution", constraintName: "FKdtqy2uar1ln372ne8u2jj27nw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-68", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "scheduled_execution_id", baseTableName: "rdoption", constraintName: "FKe75poq1n8bmqu5ireg3fjc7iu", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }


    changeSet(author: "rundeckuser (generated)", id: "3.4.0-69", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "orchestrator_id", baseTableName: "execution", constraintName: "FKgm5hu46fmti2jpbj1d7xlyw65", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "orchestrator", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-70", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "user_id", baseTableName: "node_filter", constraintName: "FKhuj3n3gy2kqoie8d8oucay4b3", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rduser", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-71", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "error_handler_id", baseTableName: "workflow_step", constraintName: "FKipk3uik8eqp54cf0ul996bu4o", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_step", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-72", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "option_id", baseTableName: "rdoption_values", constraintName: "FKnm8rfqbotpdp347xqsltkegfv", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "rdoption", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-73", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "se_id", baseTableName: "scheduled_execution_stats", constraintName: "FKpwc0snw00ehserhlpgftbu8aw", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "scheduled_execution", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-74", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "workflow_step_id", baseTableName: "workflow_workflow_step", constraintName: "FKrln65du8n7hwu3qbinnea748g", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow_step", validate: "true")
    }

    changeSet(author: "rundeckuser (generated)", id: "3.4.0-75", dbms:"mysql,mssql,postgresql") {
        addForeignKeyConstraint(baseColumnNames: "workflow_id", baseTableName: "execution", constraintName: "FKs7kalwep0lr5r39cntcu0pev6", deferrable: "false", initiallyDeferred: "false", referencedColumnNames: "id", referencedTableName: "workflow", validate: "true")
    }


}