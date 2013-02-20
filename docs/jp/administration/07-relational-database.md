% Relational Database

You can configure Rundeck to use a RDB instead of the default file-based data storage.

You must modify the `server/config/rundeck-config.properties` file, to change the `dataSource` configuration, and you will have to add the appropriate JDBC driver JAR file to the lib directory.

## Enable rdbsupport

First, you **must** enable the `rundeck.v14.rdbsupport` property:

    #note, make sure this is set to "true" if you are using Oracle or Mysql
    rundeck.v14.rdbsupport=true

This makes Rundeck use table/field names that are compatible with Oracle/Mysql.

Note: It is safe to set this to true if you are using the default file based backend, but only for a fresh install. It will cause a problem if you set it to true for an existing Rundeck 1.3 HSQLDB database.  Make sure it is set to "false" or is absent from your config file if you are upgrading from Rundeck 1.3 and using the filesystem storage.

## Customize the Datasource

The default dataSource is configured for filesystem storage using HSQLDB:

    dataSource.url = jdbc:hsqldb:file:/var/lib/rundeck/data/grailsdb;shutdown=true

Here is an example configuration to use an Oracle backend:

    dataSource.url = jdbc:oracle:thin:@localhost:1521:XE
    dataSource.driverClassName = oracle.jdbc.driver.OracleDriver
    dataSource.username = dbuser
    dataSource.password = dbpass
    dataSource.dialect = org.hibernate.dialect.Oracle10gDialect

Here is an example configuration to use Mysql:

    dataSource.url = jdbc:mysql://myserver/rundeckdb?autoReconnect=true
    dataSource.username = dbuser
    dataSource.password = dbpass

NB: for Mysql, the `autoReconnect=true` will fix a common problem where the Rundeck server's connection to Mysql is dropped after a period of inactivity, resulting in an error message: "Message: Can not read response from server. Expected to read 4 bytes, read 0 bytes before connection was unexpectedly lost."

See more about [configuring the Mysql JDBC Connector/J URL](http://dev.mysql.com/doc/refman/5.1/en/connector-j-reference-configuration-properties.html).

## Add the JDBC Driver

Copy the appropriate JDBC driver, such as "ojdbc14.jar" for Oracle into the server `lib` dir:

    cp ojdbc14.jar $RDECK_BASE/server/lib

Or:

    cp mysql-connector-java-5.1.17-bin.jar $RDECK_BASE/server/lib
