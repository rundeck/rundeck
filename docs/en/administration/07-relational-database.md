% Setting up a RDB Datasource

You can configure Rundeck to use a RDB instead of the default file-based data storage.

You must modify the `server/config/rundeck-config.properties` file, to change the `dataSource` configuration, and you will have to add the appropriate JDBC driver JAR file to the lib directory.

## Customize the Datasource

The default dataSource is configured for filesystem storage using HSQLDB:

~~~~~~ {.java}
dataSource.url = jdbc:hsqldb:file:/var/lib/rundeck/data/grailsdb;shutdown=true
~~~~~~ 

Here is an example configuration to use an Oracle backend:

~~~~~~ {.java .numberLines }
dataSource.url = jdbc:oracle:thin:@localhost:1521:XE
dataSource.driverClassName = oracle.jdbc.driver.OracleDriver
dataSource.username = dbuser
dataSource.password = dbpass
dataSource.dialect = org.hibernate.dialect.Oracle10gDialect
~~~~~~~~

Here is an example configuration to use Mysql:

~~~~~~ {.java .numberLines }
dataSource.url = jdbc:mysql://myserver/rundeckdb?autoReconnect=true
dataSource.username = dbuser
dataSource.password = dbpass
~~~~~~

NB: for Mysql, the `autoReconnect=true` will fix a common problem where the Rundeck server's connection to Mysql is dropped after a period of inactivity, resulting in an error message: "Message: Can not read response from server. Expected to read 4 bytes, read 0 bytes before connection was unexpectedly lost."

See more about [configuring the Mysql JDBC Connector/J URL](http://dev.mysql.com/doc/refman/5.1/en/connector-j-reference-configuration-properties.html).

## Add the JDBC Driver

Copy the appropriate JDBC driver, such as "ojdbc14.jar" for Oracle into the server `lib` dir:

~~~~~~ {.bash}
cp ojdbc14.jar $RDECK_BASE/server/lib
~~~~~~

Or:

~~~~~~ {.bash}
cp mysql-connector-java-5.1.17-bin.jar $RDECK_BASE/server/lib
~~~~~~ 
