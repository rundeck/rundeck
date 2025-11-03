databaseChangeLog = {

    changeSet(author: "rundeckdev", id: "run-3768-create-job-metrics-snapshot-table") {
        preConditions(onFail: "MARK_RAN") {
            not {
                tableExists(tableName: "job_metrics_snapshot")
            }
        }

        comment {
            'Create job_metrics_snapshot table for pre-computed metrics (RUN-3768)'
        }

        createTable(tableName: "job_metrics_snapshot") {
            column(name: "job_id", type: "BIGINT") {
                constraints(nullable: "false", primaryKey: "true", primaryKeyName: "pk_job_metrics_snapshot")
            }

            column(name: "snapshot_date", type: "DATE") {
                constraints(nullable: "false")
            }

            // 7-day historical totals
            column(name: "total_7day", type: "INTEGER", defaultValue: "0")
            column(name: "succeeded_7day", type: "INTEGER", defaultValue: "0")
            column(name: "failed_7day", type: "INTEGER", defaultValue: "0")
            column(name: "aborted_7day", type: "INTEGER", defaultValue: "0")
            column(name: "timedout_7day", type: "INTEGER", defaultValue: "0")
            column(name: "total_duration_7day", type: "BIGINT", defaultValue: "0")
            column(name: "min_duration_7day", type: "BIGINT")
            column(name: "max_duration_7day", type: "BIGINT")

            // JSON fields (TEXT for compatibility)
            column(name: "daily_breakdown", type: "TEXT")
            column(name: "hourly_heatmap", type: "TEXT")

            // Today's incremental counters
            column(name: "today_total", type: "INTEGER", defaultValue: "0")
            column(name: "today_succeeded", type: "INTEGER", defaultValue: "0")
            column(name: "today_failed", type: "INTEGER", defaultValue: "0")
            column(name: "today_aborted", type: "INTEGER", defaultValue: "0")
            column(name: "today_timedout", type: "INTEGER", defaultValue: "0")
            column(name: "today_duration", type: "BIGINT", defaultValue: "0")
            column(name: "today_hourly", type: "TEXT")

            // Timestamps
            column(name: "date_created", type: "TIMESTAMP", defaultValueComputed: "CURRENT_TIMESTAMP")
            column(name: "last_updated", type: "TIMESTAMP", defaultValueComputed: "CURRENT_TIMESTAMP")
        }

        createIndex(indexName: "idx_jms_snapshot_date", tableName: "job_metrics_snapshot") {
            column(name: "snapshot_date")
        }

        // Add comment explaining the table (Note: This may not work on all databases)
        // sql("COMMENT ON TABLE job_metrics_snapshot IS 'Pre-computed job execution metrics for fast retrieval (RUN-3768)'")
    }

    changeSet(author: "rundeckdev", id: "run-3768-add-foreign-key-constraint") {
        preConditions(onFail: "MARK_RAN") {
            tableExists(tableName: "job_metrics_snapshot")
            tableExists(tableName: "scheduled_execution")
            not {
                foreignKeyConstraintExists(foreignKeyName: "fk_jms_scheduled_execution")
            }
        }

        comment {
            'Add foreign key constraint from job_metrics_snapshot to scheduled_execution'
        }

        addForeignKeyConstraint(
            baseTableName: "job_metrics_snapshot",
            baseColumnNames: "job_id",
            constraintName: "fk_jms_scheduled_execution",
            referencedTableName: "scheduled_execution",
            referencedColumnNames: "id",
            onDelete: "CASCADE"  // Delete snapshot when job is deleted
        )
    }
}
