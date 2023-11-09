databaseChangeLog = {
        property name: "boolean.type", value: "BIT", dbms: "mysql,mssql,mariadb"
        property name: "boolean.type", value: "BOOLEAN", dbms: "postgresql,h2"
        property name: "boolean.type", value: "NUMBER(1, 0)", dbms: "oracle"

        property name: "bytearray.type", value: "longblob", dbms: "mysql,mariadb" // 2^32 - 1
        property name: "bytearray.type", value: "blob", dbms: "oracle,h2" // 4 GB - 1
        property name: "bytearray.type", value: "bytea", dbms: "postgresql"  //max field size of -> 1GB
        property name: "bytearray.type", value: "varbinary(max)", dbms: "mssql" // 2^31 -1 B -> 2GB

        property name: "int.type", value: "INT", dbms: "mysql, mssql,h2,mariadb"
        property name: "int.type", value: "INTEGER", dbms: "postgresql"
        property name: "int.type", value: "NUMBER(10, 0)", dbms: "oracle"

        property name: "number.type", global: "true", value: "BIGINT", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "number.type", global: "true", value: "NUMBER(19, 0)", dbms: "oracle"

        property name: "text.type", global: "true", value: "longtext", dbms: "mysql,mariadb"
        property name: "text.type", global: "true", value: "text", dbms: "postgresql"
        property name: "text.type", global: "true", value: "CLOB", dbms: "oracle"
        property name: "text.type", global: "true", value: "varchar(max)", dbms: "mssql"
        property name: "text.type", global: "true", value: "varchar(1048576)", dbms: "h2"

        property name: "timestamp.type", global: "true", value: "datetime(6)", dbms: "mysql,mariadb"
        property name: "timestamp.type", global: "true", value: "TIMESTAMP WITHOUT TIME ZONE", dbms: "postgresql"
        property name: "timestamp.type", global: "true", value: "TIMESTAMP", dbms: "oracle,h2"
        property name: "timestamp.type", global: "true", value: "datetime2", dbms: "mssql"

        property name: "varchar7.type", global: "true", value: "VARCHAR(7)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar7.type", global: "true", value: "VARCHAR2(7 CHAR)", dbms: "oracle"

        property name: "varchar8.type", global: "true", value: "VARCHAR(8)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar8.type", global: "true", value: "VARCHAR2(8 CHAR)", dbms: "oracle"

        property name: "varchar30.type", global: "true", value: "VARCHAR(30)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar30.type", global: "true", value: "VARCHAR2(30 CHAR)", dbms: "oracle"

        property name: "varchar36.type", global: "true", value: "VARCHAR(36)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar36.type", global: "true", value: "VARCHAR2(36 CHAR)", dbms: "oracle"

        property name: "varchar40.type", global: "true", value: "VARCHAR(40)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar40.type", global: "true", value: "VARCHAR2(40 CHAR)", dbms: "oracle"

        property name: "varchar64.type", global: "true", value: "VARCHAR(64)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar64.type", global: "true", value: "VARCHAR2(64 CHAR)", dbms: "oracle"

        property name: "varchar128.type", global: "true", value: "VARCHAR(128)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar128.type", global: "true", value: "VARCHAR2(128 CHAR)", dbms: "oracle"

        property name: "varchar255.type", global: "true", value: "VARCHAR(255)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar255.type", global: "true", value: "VARCHAR2(255 CHAR)", dbms: "oracle"

        property name: "varchar256.type", global: "true", value: "VARCHAR(256)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar256.type", global: "true", value: "VARCHAR2(256 CHAR)", dbms: "oracle"

        property name: "varchar265.type", global: "true", value: "VARCHAR(265)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar265.type", global: "true", value: "VARCHAR2(265 CHAR)", dbms: "oracle"

        property name: "varchar500.type", global: "true", value: "VARCHAR(500)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar500.type", global: "true", value: "VARCHAR2(500 CHAR)", dbms: "oracle"

        property name: "varchar512.type", global: "true", value: "VARCHAR(512)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar512.type", global: "true", value: "VARCHAR2(512 CHAR)", dbms: "oracle"

        property name: "varchar768.type", global: "true", value: "VARCHAR(768)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar768.type", global: "true", value: "VARCHAR2(768 CHAR)", dbms: "oracle"

        property name: "varchar1000.type", global: "true", value: "VARCHAR(1000)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar1000.type", global: "true", value: "VARCHAR2(1000 CHAR)", dbms: "oracle"

        property name: "varchar1024.type", global: "true", value: "VARCHAR(1024)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar1024.type", global: "true", value: "VARCHAR2(1024 CHAR)", dbms: "oracle"

        property name: "varchar2048.type", global: "true", value: "VARCHAR(2048)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar2048.type", global: "true", value: "VARCHAR2(2048 CHAR)", dbms: "oracle"

        property name: "varchar3000.type", global: "true", value: "VARCHAR(3000)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar3000.type", global: "true", value: "VARCHAR2(3000 CHAR)", dbms: "oracle"

        property name: "varchar3072.type", global: "true", value: "VARCHAR(3072)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar3072.type", global: "true", value: "VARCHAR2(3072 CHAR)", dbms: "oracle"

        property name: "varchar3072.type", global: "true", value: "VARCHAR(3072)", dbms: "mysql,postgresql,mssql,h2,mariadb"
        property name: "varchar3072.type", global: "true", value: "VARCHAR2(3072 CHAR)", dbms: "oracle"

        property name: "catalogName", global: "true", value: '${database.defaultCatalogName}'

        include file: 'core/HibernateIndex.groovy'
        include file: 'core/AuthToken.groovy'
        include file: 'core/BaseReport.groovy'
        include file: 'core/Execution.groovy'
        include file: 'core/JobFileRecord.groovy'
        include file: 'core/LogFileStorageRequest.groovy'
        include file: 'core/NodeFilter.groovy'
        include file: 'core/Notification.groovy'
        include file: 'core/Orchestrator.groovy'
        include file: 'core/PluginMeta.groovy'
        include file: 'core/Project.groovy'
        include file: 'core/ReferencedExecution.groovy'
        include file: 'core/RDUSER.groovy'
        include file: 'core/ReportFilter.groovy'
        include file: 'core/ScheduledExecution.groovy'
        include file: 'core/Storage.groovy'
        include file: 'core/StoredEvent.groovy'
        include file: 'core/Workflow.groovy'
        include file: 'core/RDOPTION.groovy'
        include file: 'core/Webhook.groovy'
        include file: 'core/ConstraintsIndexesKeys.groovy'
        include file: 'core/WorkflowStep.groovy'
        include file: 'core/Tag-3.4.0.groovy'
        include file: 'core/WorkflowStepVARCHARtoNVARCHAR.groovy'
        include file: 'core/Tag-3.4.4.groovy'
        include file: 'core/WorkflowStepNVARCHAR.groovy'
        include file: 'core/Tag-3.4.7.groovy'
        include file: 'core/H2AlterIdColumnsToAutoIncrementByDefault.groovy'
        include file: 'core/Tag-4.0.0.groovy'
        include file: 'core/ExecReportJcExecIdToExecutionId.groovy'
        include file: 'core/WorkflowWorkflowStepPrimaryKey.groovy'
        include file: 'core/OptionRemoveValues.groovy'
        include file: 'core/DBChangelogPrimaryKey.groovy'
        include file: 'core/BaseReportSpi.groovy'
}