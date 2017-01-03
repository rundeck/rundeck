databaseChangeLog = {
    /**
     * Per-database types.
     **/
    property name: 'text.type', value: 'longtext', dbms: 'mysql'
    property name: 'text.type', value: 'text', dbms: 'postgresql'
    // CLOB objects are not kept in memory by H2 but are streamed.
    // http://www.h2database.com/html/datatypes.html#clob_type
    property name: 'text.type', value: 'clob', dbms: 'h2'

    property name: 'boolean.type', value: 'bit(1)', dbms: 'mysql'
    property name: 'boolean.type', value: 'boolean', dbms: 'postgresql'
    property name: 'boolean.type', value: 'boolean', dbms: 'h2'

    property name: 'autoIncrement', value: 'true', dbms: 'mysql'
    property name: 'autoIncrement', value: 'true', dbms: 'postgresql'
    property name: 'autoIncrement', value: 'true', dbms: 'h2'

    property name: 'clob.type', value: 'longtext', dbms: 'mysql'
    property name: 'clob.type', value: 'text', dbms: 'postgresql'
    property name: 'clob.type', value: 'clob', dbms: 'h2'

    property name: 'blob.type', value: 'longblob', dbms: 'mysql'
    property name: 'blob.type', value: 'bytea', dbms: 'postgresql'
    // BLOB objects are not kept in memory, binary has max size of 2 GB.
    property name: 'blob.type', value: 'blob', dbms: 'h2'

    // MySQL doesn't support schemas.
    property name: 'default.schema.name', value: '', dbms: 'mysql'
    property name: 'default.schema.name', value: 'public', dbms: 'postgresql'
    property name: 'default.schema.name', value: 'PUBLIC', dbms: 'h2'

    include file: 'baseline.groovy'
    include file: 'log_file_storage_req_index.groovy'
    include file: 'remove_rdoption_id.groovy'
    include file: 'varchar_to_text_conversion.groovy'
}
