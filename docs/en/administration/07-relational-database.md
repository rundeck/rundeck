% Setting up a RDB Datasource

You can configure Rundeck to use a RDB instead of the default file-based data storage.

You must modify the `server/config/rundeck-config.properties` file, to change the `dataSource` configuration, and you will have to add the appropriate JDBC driver JAR file to the lib directory.

For Mysql-specific instructions, jump to: [Mysql Setup Guide](#mysql-setup-guide).

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

See the [Mysql Setup Guide](#mysql-setup-guide) for 
instructions on creating the rundeck database and granting access.

See more about [configuring the Mysql JDBC Connector/J URL](http://dev.mysql.com/doc/refman/5.1/en/connector-j-reference-configuration-properties.html).

## Add the JDBC Driver

Rundeck includes a JDBC driver for Mysql and H2. If you are using another database
copy the appropriate JDBC driver, such as "ojdbc14.jar" for Oracle into the server `lib` dir:

~~~~~~ {.bash}
cp ojdbc14.jar $RDECK_BASE/server/lib
~~~~~~

# Mysql setup guide

This is a simple guide for setting up Mysql for use with Rundeck.

## Install Mysql

You can "yum install" or "apt-get install" the server, or you can download rpms manually if you like. See [mysql linux installation](http://dev.mysql.com/doc/refman/5.5/en/linux-installation-native.html)

After install, run the [mysql_secure_installation script](http://dev.mysql.com/doc/refman/5.5/en/mysql-secure-installation.html). This will let prompt you to set the root password for mysql, as well as disable anonymous access.

## Setup Rundeck Database

Now you want to create a database and user access for the Rundeck server.

If it is not running, start mysqld with "service mysqld start"

Use the 'mysql' commandline tool to access the db as the root user:

    $ mysql -u root -p

Enter your root password to connect.  Once you have the mysql> prompt, enter the following commands to create the rundeck database:

    mysql> create database rundeck;
    Query OK, 1 row affected (0.00 sec)

Then "grant" access for a new user/password, and specify the hostname the Rundeck server will connect from.  if it is the same server, you can use "localhost".

    mysql> grant ALL on rundeck.* to 'rundeckuser'@'localhost' identified by 'rundeckpassword';
    Query OK, 1 row affected (0.00 sec)

You can then exit the mysql prompt.

Test access (if it's from localhost) by running:

    $ mysql -u rundeckuser -p

You can verify you can see the "rundeck" database with:

    mysql> show databases;
    +--------------------+
    | Database           |
    +--------------------+
    | information_schema |
    | rundeck            |
    +--------------------+
    2 rows in set (0.00 sec)

## Configure Rundeck

Now you need to configure Rundeck to connect to this DB as described earlier in this document: [Customize the Datasource](#customize-the-datasource).

Update your `rundeck-config.properties` and configure the datasource:

    dataSource.url = jdbc:mysql://myserver/rundeck?autoReconnect=true
    dataSource.username=rundeckuser
    dataSource.password=rundeckpassword
    
Next, [Download the mysql connector jar](http://dev.mysql.com/downloads/connector/j/).

Copy the mysql-connector-java-5.x.x-bin.jar to `$RDECK_BASE/server/lib` (for launcher install) or `$WEBAPPS/rundeck/WEB-INF/lib` (for Tomcat).

Finally you can start rundeck.  If you see a startup error about database access, make sure that the hostname that the Mysql server sees from the client is the same one you granted access to.